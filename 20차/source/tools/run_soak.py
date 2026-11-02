"""소유 장치의 단일 instrumentation을 감시하며 원문·연속성 근거를 보존합니다."""
import argparse,ctypes,hashlib,json,os,subprocess,sys,threading,time,uuid
from collections import Counter
from datetime import datetime,timedelta,timezone
from pathlib import Path
from project_paths import *
from soak_contract import HostProof,RotatingBytes,atomic_json,atomic_bytes

KST=timezone(timedelta(hours=9))


def now():return datetime.now(KST).isoformat()


def call(arguments,timeout=20):
    result=subprocess.run(PREFIX+arguments,cwd=BUILD,shell=False,capture_output=True,timeout=timeout,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode:raise RuntimeError('소유 기기 명령 실패: '+repr(arguments)+' '+result.stderr.decode('utf-8','backslashreplace'))
    return result.stdout


def clocks():
    unbiased=ctypes.c_ulonglong()
    if not ctypes.windll.kernel32.QueryUnbiasedInterruptTime(ctypes.byref(unbiased)):raise OSError('host 수면 관측 시계 미지원')
    ctypes.windll.kernel32.GetTickCount64.restype=ctypes.c_ulonglong
    return ctypes.windll.kernel32.GetTickCount64(),unbiased.value//10000


def observe(remote,folder,observations):
    result=subprocess.run(PREFIX+['shell','cat',remote+'/status.json'],cwd=BUILD,shell=False,capture_output=True,timeout=15,creationflags=subprocess.CREATE_NO_WINDOW)
    # 독립 조회가 실패해도 방금 받은 장치 원문과 명령 결과를 잃지 않습니다.
    atomic_bytes(folder/'device-observed.json',result.stdout)
    observations.write((json.dumps({'read_kst':now(),'exit_code':result.returncode,
        'sha256':hashlib.sha256(result.stdout).hexdigest(),
        'stdout':result.stdout.decode('utf-8','backslashreplace'),
        'stderr':result.stderr.decode('utf-8','backslashreplace')},ensure_ascii=False)+'\n').encode('utf-8'))
    return result


class JournalEvidence:
    """시작 후 첫 원문을 기준으로 관측된 변화만 보존하며 기존 기록을 지우지 않습니다."""
    def __init__(self,folder):
        self.folder=folder;self.previous=None;self.state={}
        self.log=RotatingBytes(folder/'error-journal-observations.jsonl')

    def observe(self,remote):
        result=subprocess.run(PREFIX+['shell','cat',remote+'/errors.txt'],cwd=BUILD,shell=False,
            capture_output=True,timeout=15,creationflags=subprocess.CREATE_NO_WINDOW)
        at=now();raw=result.stdout;digest=hashlib.sha256(raw).hexdigest()
        atomic_bytes(self.folder/'errors-observed.txt',raw)
        self.log.write((json.dumps({'observed_kst':at,'exit_code':result.returncode,'sha256':digest,
            'stdout':raw.decode('utf-8','backslashreplace'),'stderr':result.stderr.decode('utf-8','backslashreplace')},
            ensure_ascii=False)+'\n').encode('utf-8'))
        if result.returncode:raise RuntimeError('오류 일지 수집 실패: '+(raw+result.stderr).decode('utf-8','backslashreplace'))
        text=raw.decode('utf-8')
        if self.previous is None:
            atomic_bytes(self.folder/'errors-initial.txt',raw)
            self.state=dict(initial_sha256=digest,current_sha256=digest,observations=0,changes=0,
                initial_observed_kst=at,observed_kst=at,baseline_after_start=True,new_line_count=0,
                initial_nonempty=bool(raw),initial_path=str(self.folder/'errors-initial.txt'),
                current_path=str(self.folder/'errors-observed.txt'),last_changed_kst=None)
        elif raw!=self.previous:
            old=Counter(line for line in self.previous.decode('utf-8').split('\n') if line)
            added=Counter(line for line in text.split('\n') if line)-old
            self.state['new_line_count']+=sum(added.values());self.state['changes']+=1
            self.state['last_changed_kst']=at
        self.state.update(current_sha256=digest,observations=self.state['observations']+1,observed_kst=at)
        self.previous=raw
        return dict(self.state)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--seconds',type=int,choices=(300,86400),required=True)
    parser.add_argument('--run-id',required=True);arguments=parser.parse_args()
    if not __import__('re').fullmatch('[A-Za-z0-9_-]+',arguments.run_id):raise ValueError('실행 ID 형식 오류')
    folder=CONTROL/arguments.run_id
    if folder.exists():raise RuntimeError('기존 실행 덮어쓰기 금지')
    folder.mkdir(parents=True);raw=RotatingBytes(folder/'instrumentation.bin');events=RotatingBytes(folder/'heartbeat.jsonl')
    observations=RotatingBytes(folder/'device-observations.jsonl')
    journal=JournalEvidence(folder)
    state_path=CONTROL/'status.json';process=None;reader=None;power_requested=False;final={'result':'PREPARING'}
    started=time.monotonic();start_clocks=clocks();start_wall=datetime.now(KST)
    proof=HostProof(arguments.run_id,arguments.seconds,started)
    remote='/sdcard/Android/data/kr.yunseok.touchmacro/files/pxe20-'+arguments.run_id
    argv=PREFIX+['shell','am','instrument','-w','-r','-e','class','kr.yunseok.touchmacro.LongRunOcrTest',
                 '-e','soak_seconds',str(arguments.seconds),'-e','run_id',arguments.run_id,
                 'kr.yunseok.touchmacro.test/androidx.test.runner.AndroidJUnitRunner']
    metadata={'run_id':arguments.run_id,'requested_seconds':arguments.seconds,'host_pid':os.getpid(),'argv':argv,'cwd':str(BUILD),
              'started_kst':start_wall.isoformat(),'deadline_kst':(start_wall+timedelta(seconds=arguments.seconds)).isoformat(),
              'serial':'emulator-5598','adb_port':5043,'product_sha256':PC_SHA,'display_required':False,
              'same_process_required':True,'phase':'PREFLIGHT' if arguments.seconds==300 else 'MAIN',
              'limits':{'heartbeat_seconds':30,'gap_seconds':90,'command_seconds':480 if arguments.seconds==300 else 87000}}
    routing=json.loads((ROOT/'.codex/routing-state/df1ea7273e46d16ba9664afc.json').read_text(encoding='utf-8'))
    assert routing['task_id']=='pxe20-24h-soak-20261102'
    metadata['counters_at_start']={key:routing[key] for key in ('repair_used','input_fix_used','stop_failures','verification_failures')}
    test_apk=BUILD/'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
    inputs=[ROOT/'androidTest/LongRunOcrTest.java',ROOT/'tools/run_soak.py',ROOT/'tools/soak_contract.py',test_apk]
    metadata['inputs']=[{'path':str(path),'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()} for path in inputs]
    atomic_json(folder/'launch.json',metadata)
    final={**metadata,'result':'PREPARING','updated_kst':now(),'ocr_verified':False,'multiple_cycles_verified':False}
    atomic_json(folder/'status.json',final);atomic_json(state_path,final)
    try:
        assert hashlib.sha256(PC.read_bytes()).hexdigest()==PC_SHA,'19차 제품 파일 변경'
        assert call(['shell','getprop','ro.boot.qemu.avd_name']).decode().strip()=='PXE19'
        location=call(['shell','pm','path','kr.yunseok.touchmacro']).decode().strip()
        if not location.startswith('package:') or '\n' in location:raise RuntimeError('단일 제품 APK 경로 아님')
        target=folder/'installed19.apk';call(['pull',location[len('package:'):],str(target)])
        assert hashlib.sha256(target.read_bytes()).hexdigest()==PC_SHA,'실제 설치 제품 SHA 불일치'
        power=ctypes.windll.kernel32.SetThreadExecutionState(0x80000001)
        if not power:raise OSError('프로세스 절전 방지 요청 실패')
        power_requested=True;metadata['power_request_succeeded']=True;atomic_json(folder/'launch.json',metadata)
        # 준비 검사는 실제 연속 시간에 포함하지 않습니다. 이 시각부터 단일 원격 실행을 시작합니다.
        started=time.monotonic();start_clocks=clocks();proof=HostProof(arguments.run_id,arguments.seconds,started)
        start_wall=datetime.now(KST);metadata['instrument_started_kst']=start_wall.isoformat()
        metadata['deadline_kst']=(start_wall+timedelta(seconds=arguments.seconds)).isoformat();atomic_json(folder/'launch.json',metadata)
        process=subprocess.Popen(argv,cwd=BUILD,shell=False,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0,creationflags=subprocess.CREATE_NO_WINDOW)
        metadata['adb_client_pid']=process.pid;atomic_json(folder/'launch.json',metadata)
        tail=bytearray()
        def collect():
            while True:
                data=process.stdout.read(4096)
                if not data:break
                raw.write(data);tail.extend(data)
                if len(tail)>65536:del tail[:-65536]
        reader=threading.Thread(target=collect,daemon=True);reader.start();last_tick=started;next_tick=started
        while True:
            clock=time.monotonic();host_gap=clock-last_tick
            if clock-started>metadata['limits']['command_seconds']:raise TimeoutError('단일 시험 명령 상한 초과')
            if clock>=next_tick:
                last_tick=clock;next_tick=clock+30;current_clocks=clocks()
                sleep_ms=(current_clocks[0]-start_clocks[0])-(current_clocks[1]-start_clocks[1])
                status_read=observe(remote,folder,observations)
                if status_read.returncode:
                    if status_read.returncode==1 and b'No such file or directory' in status_read.stdout+status_read.stderr and clock-started<75 and not proof.saw_running:
                        if process.poll() is not None:
                            reader.join(timeout=5)
                            raise RuntimeError('장치 상태 생성 전 instrumentation 종료: '+str(process.returncode))
                        time.sleep(1);next_tick=0;continue
                    raise RuntimeError('장치 상태 파일 명령 실패: '+(status_read.stdout+status_read.stderr).decode('utf-8','backslashreplace'))
                result=status_read.stdout
                try:device=json.loads(result.decode('utf-8'))
                except (ValueError,UnicodeError):
                    if clock-started<75 and not proof.saw_running:
                        time.sleep(1);next_tick=0;continue
                    raise RuntimeError('실제 장치 상태 JSON 읽기 실패')
                journal_state=journal.observe(remote)
                # 최종 상태는 정상 계측 종료보다 먼저 쓰이며, 종료 뒤에는 앱 PID가 없어집니다.
                verdict=proof.accept(device,clock,host_gap,sleep_ms)
                final={**metadata,**verdict,'device':device,'device_remote_path':remote,
                       'device_status_sha256':hashlib.sha256(result).hexdigest(),
                       'device_observed_path':str(folder/'device-observed.json')}
                if verdict['result'] not in ('RUNNING','PASS'):raise RuntimeError(verdict['reason'])
                if verdict['result']=='RUNNING':
                    try:current_pid=call(['shell','pidof','-s','kr.yunseok.touchmacro']).decode().strip()
                    except RuntimeError:
                        if process.poll()!=0:raise
                        # RUNNING을 읽은 직후 정상 종료한 경우에만 마지막 원문을 한 번 재수집합니다.
                        last=observe(remote,folder,observations)
                        if last.returncode:raise RuntimeError('종료 경계 장치 최종 상태 읽기 실패')
                        result=last.stdout;device=json.loads(result.decode('utf-8'))
                        journal_state=journal.observe(remote)
                        final.update(device=device,device_status_sha256=hashlib.sha256(result).hexdigest())
                        clock=time.monotonic();verdict=proof.accept(device,clock,clock-last_tick,sleep_ms)
                        if verdict['result']!='PASS':raise RuntimeError('계측 종료 뒤 최종 PASS 상태 없음: '+verdict['reason'])
                    else:
                        if str(device['pid'])!=current_pid:raise RuntimeError('독립 PID 대조 불일치')
                current_boot=call(['shell','cat','/proc/sys/kernel/random/boot_id']).decode().strip()
                if device['boot_id']!=current_boot:verdict={'result':'FAIL','reason':'독립 boot 대조 불일치'}
                final={**metadata,**verdict,'updated_kst':now(),'host_elapsed_seconds':clock-started,'host_sleep_ms':sleep_ms,'device':device,'device_remote_path':remote,
                       'device_status_sha256':hashlib.sha256(result).hexdigest(),'device_observed_path':str(folder/'device-observed.json'),'power_request_succeeded':power_requested,
                       'ocr_verified':device['ocr_count']>0 and device['capture_ms']>0,
                       'multiple_cycles_verified':device['cycles']>=3 and device['rounds']>=45}
                final.update(error_journal=journal_state,journal_warning=journal_state['initial_nonempty'] or journal_state['changes']>0)
                atomic_json(folder/'device-latest.json',device);atomic_json(folder/'status.json',final);atomic_json(state_path,final)
                events.write((json.dumps({'updated_kst':final['updated_kst'],'result':verdict['result'],
                    'host_elapsed_seconds':final['host_elapsed_seconds'],'host_sleep_ms':sleep_ms,'device':device},ensure_ascii=False)+'\n').encode('utf-8'))
                if verdict['result'] not in ('RUNNING','PASS'):raise RuntimeError(verdict['reason'])
                if verdict['result']=='PASS':
                    process.wait(timeout=20);reader.join(timeout=5)
                    if process.returncode!=0 or b'OK (1 test)' not in tail or b'FAILURES!!!' in tail:raise RuntimeError('instrumentation 최종 성공 표식 없음')
                    break
            if process.poll() is not None:
                # 마지막 상태는 직전 heartbeat 이후에도 쓰일 수 있으므로 한 번 즉시 수집합니다.
                if proof.previous and proof.previous.get('result')=='PASS':break
                next_tick=0
            time.sleep(.5)
        journal_state=journal.observe(remote)
        atomic_bytes(folder/'errors-final.txt',journal.previous)
        final.update(error_journal=journal_state,journal_warning=journal_state['initial_nonempty'] or journal_state['changes']>0)
    except BaseException as error:
        final={**metadata,**final,'result':'INCONCLUSIVE' if final.get('result')=='INCONCLUSIVE' else 'FAIL',
               'error_type':type(error).__name__,'error':str(error),'updated_kst':now(),'host_elapsed_seconds':time.monotonic()-started}
        atomic_json(folder/'failure.json',final)
        if process is not None:
            try:call(['shell','am','force-stop','kr.yunseok.touchmacro'],timeout=10)
            except BaseException as cleanup:final['cleanup_error']=type(cleanup).__name__+': '+str(cleanup)
        raise
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        if reader is not None:reader.join(timeout=5)
        final['power_release_succeeded']=bool(ctypes.windll.kernel32.SetThreadExecutionState(0x80000000)) if power_requested else None
        final['finished_kst']=now();final['host_elapsed_seconds']=time.monotonic()-started
        atomic_json(folder/'status.json',final);atomic_json(state_path,final)
        print(json.dumps(final,ensure_ascii=False),flush=True)


if __name__=='__main__':main()
