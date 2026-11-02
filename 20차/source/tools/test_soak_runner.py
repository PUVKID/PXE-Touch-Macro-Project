"""연속 시험의 완료·단절·정체 판정 실패 경로를 검증합니다."""
import copy
import contextlib
import hashlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch
from soak_contract import assess, HostProof, RotatingBytes, atomic_json


def sample(**changes):
    value = dict(run_id='검사', seq=1, result='RUNNING', pid=10, boot_id='부팅', generation=2,
                 elapsed_ms=30_000, uptime_ms=130_000, started_elapsed_ms=100_000,
                 cycles=0, rounds=4, entries=1, backs=0, ocr_count=12, capture_ms=20,
                 errors='', last_progress_elapsed_ms=129_000, requested_seconds=86400)
    value.update(changes)
    return value


class ContinuityTests(unittest.TestCase):
    def test_a_device_pass_before_full_duration_cannot_pass(self):
        self.assertEqual('FAIL', assess(None, sample(result='PASS'), 1)['result'])

    def test_full_duration_alone_does_not_turn_running_into_pass(self):
        self.assertEqual('RUNNING', assess(None, sample(elapsed_ms=86_400_000, uptime_ms=86_500_000,
                                                       last_progress_elapsed_ms=86_499_000), 1)['result'])

    def test_restart_cannot_join_elapsed_time(self):
        old = sample()
        for key, value in [('pid', 11), ('boot_id', '다른 부팅'), ('generation', 3), ('run_id', '다른 실행')]:
            with self.subTest(key=key):
                self.assertEqual('FAIL', assess(old, sample(seq=2, **{key:value}), 1)['result'])

    def test_host_gap_and_device_sleep_make_continuity_inconclusive(self):
        self.assertEqual('INCONCLUSIVE', assess(sample(), sample(seq=2), 91)['result'])
        self.assertEqual('INCONCLUSIVE', assess(sample(), sample(seq=2, elapsed_ms=60_000, uptime_ms=135_000), 30)['result'])

    def test_regression_duplicate_sequence_and_boolean_counts_are_rejected(self):
        for changed in [dict(ocr_count=1), dict(seq=1, ocr_count=13), dict(cycles=True), dict(errors='중복 입력')]:
            with self.subTest(changed=changed):
                arguments=dict(seq=2);arguments.update(changed)
                self.assertEqual('FAIL', assess(sample(), sample(**arguments), 1)['result'])

    def test_identical_checkpoint_can_be_read_twice_without_false_restart(self):
        self.assertEqual('RUNNING', assess(sample(), sample(), 1)['result'])

    def test_long_stall_fails_but_short_stall_is_warning(self):
        self.assertEqual('FAIL', assess(None, sample(elapsed_ms=400_000, last_progress_elapsed_ms=100_000), 1)['result'])
        self.assertEqual('RUNNING', assess(None, sample(elapsed_ms=280_000, last_progress_elapsed_ms=100_000), 1)['result'])
        self.assertTrue(assess(None, sample(elapsed_ms=280_000, last_progress_elapsed_ms=100_000), 1)['warning'])

    def test_completed_boundary_requires_exact_fifteen_and_real_ocr(self):
        good = sample(result='PASS', seq=3000, elapsed_ms=86_400_001, cycles=800, backs=800,
                      entries=800, rounds=12000, ocr_count=25000, last_progress_elapsed_ms=86_499_000)
        self.assertEqual('PASS', assess(None, good, 1)['result'])
        for changed in [dict(rounds=11999), dict(ocr_count=0), dict(backs=799)]:
            wrong=copy.copy(good);wrong.update(changed)
            self.assertEqual('FAIL', assess(None, wrong, 1)['result'])

    def test_atomic_status_and_rotated_raw_logs_are_bounded(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder);log=RotatingBytes(path/'원문.bin', limit=16, copies=2)
            for _ in range(20):log.write(b'12345678')
            self.assertEqual(3, len(list(path.glob('원문.bin*'))))
            self.assertTrue(all(item.stat().st_size<=16 for item in path.glob('원문.bin*')))
            atomic_json(path/'status.json', {'상태':'검증 중'})
            self.assertIn('검증 중', (path/'status.json').read_text(encoding='utf-8'))
            self.assertFalse(list(path.glob('*.tmp')))


class HostClockTests(unittest.TestCase):
    def running(self,**changes):
        value=sample(seq=1,elapsed_ms=0,uptime_ms=100000,started_uptime_ms=100000,
                     last_progress_elapsed_ms=100000,observed_generation=2,running=True,watching=True,paused=False,
                     rounds=0,entries=0,ocr_count=0,capture_ms=0,requested_seconds=300)
        value.update(changes);return value

    def completed(self):
        return self.running(seq=2,result='PASS',elapsed_ms=300001,uptime_ms=400001,last_progress_elapsed_ms=400000,
                            cycles=3,entries=3,backs=3,rounds=45,ocr_count=100,capture_ms=80000,
                            observed_generation=3,running=False,watching=False,stop_ms=100)

    def test_preexisting_completed_file_is_not_an_initial_running_observation(self):
        self.assertEqual('FAIL',HostProof('검사',300,1000).accept(self.completed(),1301,1)['result'])

    def test_forged_duration_cannot_skip_host_real_time(self):
        proof=HostProof('검사',300,1000);self.assertEqual('RUNNING',proof.accept(self.running(),1000,0)['result'])
        self.assertEqual('FAIL',proof.accept(self.completed(),1001,1)['result'])

    def test_full_host_and_device_duration_with_planned_final_stop_can_pass(self):
        proof=HostProof('검사',300,1000);proof.accept(self.running(),1000,0)
        self.assertEqual('PASS',proof.accept(self.completed(),1300.1,30)['result'])

    def test_unknown_id_missing_uptime_and_observed_generation_change_fail(self):
        for changed in (dict(run_id='다른 실행'),dict(observed_generation=4)):
            proof=HostProof('검사',300,1000)
            self.assertEqual('FAIL',proof.accept(self.running(**changed),1000,0)['result'])
        value=self.running();del value['started_uptime_ms']
        self.assertEqual('FAIL',HostProof('검사',300,1000).accept(value,1000,0)['result'])

    def test_repeated_unchanged_device_file_has_a_ninety_second_limit(self):
        proof=HostProof('검사',300,1000);proof.accept(self.running(),1000,0)
        self.assertEqual('RUNNING',proof.accept(self.running(),1030,30)['result'])
        self.assertEqual('INCONCLUSIVE',proof.accept(self.running(),1091,30)['result'])

    def test_host_sleep_is_not_joined_to_continuous_time(self):
        proof=HostProof('검사',300,1000);proof.accept(self.running(),1000,0)
        self.assertEqual('INCONCLUSIVE',proof.accept(self.running(),1030,30,sleep_ms=3000)['result'])


class CollectorBoundaryTests(unittest.TestCase):
    """ADB·프로세스·시계 경계만 대역으로 두고 실제 수집기 전체를 실행합니다."""
    running=HostClockTests.running
    completed=HostClockTests.completed
    def collect(self, missing_pid=False, last_running=False, exit_code=0, different_boot=False):
        import run_soak
        with tempfile.TemporaryDirectory() as temporary:
            base=Path(temporary);root=base/'source';build=base/'build';control=base/'control'
            (root/'.codex/routing-state').mkdir(parents=True);(root/'androidTest').mkdir()
            (root/'tools').mkdir();control.mkdir()
            (root/'.codex/routing-state/df1ea7273e46d16ba9664afc.json').write_text(json.dumps(dict(
                task_id='pxe20-24h-soak-20261102',repair_used=5,input_fix_used=0,stop_failures=0,verification_failures=0)),encoding='utf-8')
            for name in ('androidTest/LongRunOcrTest.java','tools/run_soak.py','tools/soak_contract.py'):
                (root/name).write_text('검사 입력',encoding='utf-8')
            apk=build/'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
            apk.parent.mkdir(parents=True);apk.write_bytes(b'fixture')
            product=base/'product.apk';product.write_bytes(b'fixture')
            states=[self.running(run_id='regression',seq=index+1,elapsed_ms=index*30000,
                uptime_ms=100000+index*30000,last_progress_elapsed_ms=100000+index*30000)
                for index in range(10)]
            terminal=self.completed();terminal.update(run_id='regression',seq=12)
            if last_running:
                states.append(self.running(run_id='regression',seq=11,elapsed_ms=300000,
                    uptime_ms=400000,last_progress_elapsed_ms=400000))
            states.append(terminal)
            cursor={'index':-1};clock={'value':1000.0}
            class Process:
                pid=123;returncode=None
                stdout=io.BytesIO(b'OK (1 test)\nINSTRUMENTATION_CODE: -1\n')
                def poll(self):
                    if cursor['index']>=len(states)-1-int(last_running):self.returncode=exit_code
                    return self.returncode
                def wait(self,timeout=None):self.returncode=exit_code;return exit_code
                def terminate(self):self.returncode=exit_code
                def kill(self):self.returncode=exit_code
            process=Process()
            def read_status(argv,**kwargs):
                if argv[-1].endswith('/errors.txt'):
                    return SimpleNamespace(returncode=0,stdout=b'',stderr=b'')
                cursor['index']+=1;state=states[cursor['index']]
                clock['value']=1000+state['elapsed_ms']/1000
                return SimpleNamespace(returncode=0,stdout=json.dumps(state).encode('utf-8'),stderr=b'')
            def call(arguments,timeout=20):
                if arguments[:2]==['shell','getprop']:return b'PXE19'
                if arguments[:3]==['shell','pm','path']:return b'package:/fixture.apk'
                if arguments[0]=='pull':Path(arguments[2]).write_bytes(b'fixture');return b''
                if arguments[:2]==['shell','pidof']:
                    if missing_pid and cursor['index']>=len(states)-1-int(last_running):
                        raise RuntimeError('소유 기기 명령 실패: pidof 종료코드 1')
                    return b'10'
                if arguments[:2]==['shell','cat']:
                    return ('다른 부팅' if different_boot and cursor['index']==len(states)-1 else '부팅').encode('utf-8')
                if arguments[:3]==['shell','am','force-stop']:return b''
                raise AssertionError(arguments)
            error=None
            with contextlib.ExitStack() as stack:
                for name,value in dict(ROOT=root,BUILD=build,CONTROL=control,PC=product,
                    PC_SHA=hashlib.sha256(b'fixture').hexdigest()).items():stack.enter_context(patch.object(run_soak,name,value))
                stack.enter_context(patch.object(run_soak.sys,'argv',['run_soak.py','--seconds','300','--run-id','regression']))
                stack.enter_context(patch.object(run_soak,'call',side_effect=call))
                stack.enter_context(patch.object(run_soak,'clocks',return_value=(0,0)))
                stack.enter_context(patch.object(run_soak.subprocess,'Popen',return_value=process))
                stack.enter_context(patch.object(run_soak.subprocess,'run',side_effect=read_status))
                stack.enter_context(patch.object(run_soak.time,'monotonic',side_effect=lambda:clock['value']))
                stack.enter_context(patch.object(run_soak.time,'sleep',side_effect=lambda seconds:clock.update(value=clock['value']+30)))
                stack.enter_context(patch.object(run_soak.ctypes.windll.kernel32,'SetThreadExecutionState',return_value=1))
                stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
                try:run_soak.main()
                except BaseException as caught:error=caught
            folder=control/'regression';status=json.loads((folder/'status.json').read_text(encoding='utf-8'))
            observed=folder/'device-observed.json';raw=observed.read_bytes() if observed.exists() else None
            return status,error,raw

    def test_successful_finish_allows_absent_live_pid(self):
        status,error,raw=self.collect(missing_pid=True)
        self.assertIsNone(error,str(error));self.assertEqual('PASS',status['result'])
        self.assertEqual('PASS',json.loads(raw)['result'])

    def test_running_read_can_race_with_completed_instrumentation(self):
        status,error,_=self.collect(missing_pid=True,last_running=True)
        self.assertIsNone(error,str(error));self.assertEqual('PASS',status['result'])

    def test_raw_terminal_checkpoint_survives_independent_identity_failure(self):
        status,error,raw=self.collect(different_boot=True)
        self.assertIsNotNone(error);self.assertEqual('FAIL',status['result'])
        self.assertIsNotNone(raw,'독립 조회 실패 전에 장치 원문을 보존해야 합니다')
        self.assertEqual('PASS',json.loads(raw)['result'])

    def test_nonzero_instrumentation_exit_cannot_pass(self):
        status,error,_=self.collect(missing_pid=True,exit_code=1)
        self.assertIsNotNone(error);self.assertEqual('FAIL',status['result'])


class JournalEvidenceTests(unittest.TestCase):
    def test_initial_history_and_added_warning_preserve_raw_without_changing_verdict(self):
        import run_soak
        with tempfile.TemporaryDirectory() as temporary:
            folder=Path(temporary);journal=run_soak.JournalEvidence(folder)
            initial='기존 복구 경고\n'.encode('utf-8');changed=initial+'새 복구 경고\n'.encode('utf-8')
            with patch.object(run_soak.subprocess,'run',side_effect=[
                SimpleNamespace(returncode=0,stdout=value,stderr=b'') for value in (initial,initial,changed)]):
                first=journal.observe('/fixture');same=journal.observe('/fixture');last=journal.observe('/fixture')
            self.assertTrue(first['baseline_after_start']);self.assertTrue(first['initial_nonempty'])
            self.assertEqual(0,first['new_line_count']);self.assertEqual(0,same['changes'])
            self.assertEqual(1,last['changes']);self.assertEqual(1,last['new_line_count']);self.assertEqual(3,last['observations'])
            self.assertEqual(initial,(folder/'errors-initial.txt').read_bytes())
            self.assertEqual(changed,(folder/'errors-observed.txt').read_bytes())
            self.assertEqual(3,len((folder/'error-journal-observations.jsonl').read_text(encoding='utf-8').strip().split('\n')))

    def test_journal_collection_failure_is_not_empty_success(self):
        import run_soak
        with tempfile.TemporaryDirectory() as temporary:
            journal=run_soak.JournalEvidence(Path(temporary))
            with patch.object(run_soak.subprocess,'run',return_value=SimpleNamespace(returncode=1,stdout=b'',stderr=b'permission denied')):
                with self.assertRaisesRegex(RuntimeError,'오류 일지 수집 실패'):journal.observe('/fixture')
            self.assertIn('permission denied',(Path(temporary)/'error-journal-observations.jsonl').read_text(encoding='utf-8'))


class BackgroundLifetimeTests(unittest.TestCase):
    def test_owned_background_child_is_outside_callers_job(self):
        import ctypes
        import ctypes.wintypes as types
        import project_paths
        import runpy
        import sys
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);(root/'tools').mkdir();control=root/'control';control.mkdir()
            (root/'tools/같은폴더도우미.py').write_text('문구="한국어 출력 확인"\n',encoding='utf-8')
            (root/'tools/launch_emulator.py').write_text('import time,sys\nfrom 같은폴더도우미 import 문구\nprint(문구,flush=True)\nprint("한국어 오류 출력",file=sys.stderr,flush=True)\ntime.sleep(30)\n',encoding='utf-8')
            with patch.object(project_paths,'ROOT',root),patch.object(project_paths,'CONTROL',control),\
                 patch.object(sys,'argv',['start_background.py','emulator']),contextlib.redirect_stdout(io.StringIO()):
                runpy.run_path(str(Path(__file__).with_name('start_background.py')),run_name='__main__')
            record=json.loads((control/'emulator-supervisor-background.json').read_text(encoding='utf-8'))
            kernel=ctypes.WinDLL('kernel32',use_last_error=True)
            kernel.OpenProcess.argtypes=[types.DWORD,types.BOOL,types.DWORD];kernel.OpenProcess.restype=types.HANDLE
            kernel.IsProcessInJob.argtypes=[types.HANDLE,types.HANDLE,ctypes.POINTER(types.BOOL)]
            kernel.TerminateProcess.argtypes=[types.HANDLE,types.UINT];kernel.CloseHandle.argtypes=[types.HANDLE]
            kernel.WaitForSingleObject.argtypes=[types.HANDLE,types.DWORD]
            handle=kernel.OpenProcess(0x101001,False,record['pid']);inside=types.BOOL()
            try:
                self.assertTrue(handle);self.assertTrue(kernel.IsProcessInJob(handle,None,ctypes.byref(inside)))
                self.assertFalse(inside.value,'장기 시험 자식이 호출 도구의 종료 작업 그룹에 남았습니다')
                output=(control/'emulator-supervisor-host-output.txt').read_text(encoding='utf-8')
                self.assertIn('한국어 출력 확인',output)
                self.assertIn('한국어 오류 출력',output)
            finally:
                if handle:
                    kernel.TerminateProcess(handle,0)
                    self.assertEqual(0,kernel.WaitForSingleObject(handle,5000))
                    kernel.CloseHandle(handle)


if __name__=='__main__':
    unittest.main(verbosity=2)
