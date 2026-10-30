"""동결된 최종본을 독립 실행하고 검사 원문과 전후 해시를 보존합니다."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
ROOT = Path('D:/2026/PXE Project/14차/source')
EVIDENCE = ROOT / 'evidence'
sys.path.insert(0, 'C:/Users/PUVKID/.codex/hooks')
from routing_stop import source_snapshot

PYTHON = 'C:/Users/PUVKID/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
ADB = 'D:/PXE14Build/android/android-sdk/platform-tools/adb.exe'
SESSION = '01a131ad-d167-7873-9aa6-4d81ff131c60'
TASK = 'pxe14-redesign-20261026'

def now():
    return datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat()

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def save(name, value):
    (EVIDENCE / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def snapshot():
    return {'source': source_snapshot(ROOT), 'master_sha256': sha(ROOT / 'tools/required-check-plan.json'),
            'pc_sha256': sha(ROOT.parent / 'PXE_14차_v1.4_PC용.apk'),
            'phone_sha256': sha(ROOT.parent / 'PXE_14차_v1.4_노트10플러스.apk'),
            'test_apk_sha256': sha(ROOT / 'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk')}

def run(name, argv, timeout, cwd=ROOT, env=None, expected=None):
    started, clock = now(), time.monotonic()
    process = subprocess.Popen(argv, cwd=cwd, env=env, shell=False, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    captured = bytearray()
    original = 0
    raw_hash = hashlib.sha256()
    limit = 131072
    def drain():
        nonlocal original
        while block := process.stdout.read(8192):
            original += len(block)
            raw_hash.update(block)
            captured.extend(block[:max(0, limit-len(captured))])
    reader = threading.Thread(target=drain, daemon=True)
    reader.start()
    timed_out = False
    try:
        code = process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        timed_out = True
        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], shell=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=5)
        code = process.wait(timeout=5)
    reader.join(timeout=5)
    raw = bytes(captured)
    invalid_utf8 = None
    try:
        output = raw.decode('utf-8', errors='strict')
    except UnicodeDecodeError as error:
        if original > len(raw) and error.reason == 'unexpected end of data':
            raw = raw[:error.start]
            output = raw.decode('utf-8', errors='strict')
        else:
            output = raw.decode('utf-8', errors='replace')
            invalid_utf8 = str(error)
    (EVIDENCE / (name+'.txt')).write_bytes(raw)
    actual = sum(int(n) for n in re.findall(r'OK \((\d+) tests?\)', output))
    result = {'session': SESSION, 'task': TASK, 'repair_used': 6, 'input_fix_used': 12,
              'started_kst': started, 'ended_kst': now(), 'argv': argv, 'cwd': str(cwd),
              'exit_code': code, 'timeout_seconds': timeout, 'timed_out': timed_out,
              'elapsed_seconds': round(time.monotonic()-clock, 3), 'original_bytes': original,
              'captured_bytes': len(raw), 'capture_limit_bytes': limit, 'truncated': original>len(raw),
              'original_sha256': raw_hash.hexdigest(), 'captured_sha256': hashlib.sha256(raw).hexdigest(),
              'output_invalid_utf8': invalid_utf8, 'reader_alive': reader.is_alive(),
              'expected_tests': expected, 'actual_tests': actual, 'product_changes_by_verifier': 0}
    failed_marker = bool(re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS_CODE: -', output))
    result['passed'] = code==0 and not timed_out and not invalid_utf8 and not reader.is_alive() and not failed_marker and (expected is None or actual==expected)
    save(name+'.json', result)
    print(json.dumps({'step': name, **result}, ensure_ascii=False), flush=True)
    if not result['passed']:
        save('verifier-final6-failure-first.json', {**result, 'error_original': output, 'changes_at_failure': '제품·검사 수정0, 독립 검증 산출물만 기록'})
        raise RuntimeError(name+' 독립 검사 실패: 원문 선기록 완료')
    return result, output

def main():
    before = snapshot()
    save('verifier-final6-before.json', {'kst': now(), **before})
    expected = {'digest':'4e68356d37dff2e826e4dabde0e4a575b7c67f62d4c388b5b5e226028ab22094', 'global_digest':'08ee321a29c67f920341a05dbfca6e3616477d930d0ecb261d1118a581569a43'}
    assert all(before['source'][k]==v for k,v in expected.items()), '등록 소스/전역 해시 불일치'
    assert before['master_sha256']=='1145f82ac2d7c6729706b3fd117205b9fefdc71128b28d251f457b64a0c09ebd'
    env = os.environ.copy()
    env.update(JAVA_HOME='D:/PXE14Build/tools/jdk-17.0.20.1+1', ANDROID_HOME='D:/PXE14Build/android/android-sdk', NEX_SIGNING_DIR='D:/PXE14Build/signing')
    gradle, _ = run('verifier-final6-gradle', ['D:/PXE14Build/tools/gradle-8.13/bin/gradle.bat', ':app:testDebugUnitTest', ':app:lintDebug', '--rerun-tasks', '--console=plain', '--no-watch-fs', '--project-cache-dir', 'work/.gradle'], 180, Path('D:/PXE14Build/source'), env)
    xmls = sorted((ROOT/'work/build/app/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    units = [ET.parse(p).getroot().attrib for p in xmls]
    unit_summary = {'classes':len(units), **{k:sum(int(x.get(k,'0')) for x in units) for k in ['tests','failures','errors','skipped']}}
    lint = ET.parse(ROOT/'work/build/app/reports/lint-results-debug.xml').getroot()
    lint_summary = {'issues':len(lint.findall('issue'))}
    assert unit_summary=={'classes':12,'tests':80,'failures':0,'errors':0,'skipped':0}, unit_summary
    assert lint_summary['issues']==0, lint_summary
    run('verifier-final6-device-identity', [ADB,'-P','5043','-s','emulator-5598','shell','getprop','ro.boot.qemu.avd_name'], 15)
    assert (EVIDENCE/'verifier-final6-device-identity.txt').read_text(encoding='utf-8').strip()=='PXE14'
    plan = json.loads((ROOT/'tools/required-check-plan.json').read_text(encoding='utf-8-sig'))
    checks = plan['checks']
    device_start = time.monotonic()
    batches = []
    for index, name in [(6,'upgrade2'),(5,'ui6'),(7,'confirmation4'),(3,'core20'),(4,'boundary14')]:
        check = checks[index]
        remaining = 720-(time.monotonic()-device_start)
        assert remaining>0, '공유 기기 예산 소진'
        argv = [PYTHON if arg=='{python}' else arg for arg in check['argv']]
        result, _ = run('verifier-final6-'+name, argv, min(check['timeout_seconds'],remaining), expected=check['expected_tests'])
        batches.append(result)
    after = snapshot()
    save('verifier-final6-after.json', {'kst':now(), **after})
    summary = {'session':SESSION,'task':TASK,'repair_used':6,'input_fix_used':12,'verification_failures':0,
               'completed_kst':now(),'before':before,'after':after,'unchanged':before==after,'gradle':gradle,
               'unit_summary':unit_summary,'lint_summary':lint_summary,'device_batches':batches,
               'total_tests':sum(x['actual_tests'] for x in batches),'device_total_seconds':round(time.monotonic()-device_start,3),
               'device_budget_seconds':720,'additional_prefs_reset_between_batches':False}
    save('verifier-final6-summary.json', summary)
    assert before==after, '독립 검사 전후 소스/전역/APK/명세 변경'
    print('독립 최종46개 및 Gradle80/lint0 완료', flush=True)

if __name__=='__main__':
    main()
