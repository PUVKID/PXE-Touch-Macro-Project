"""승인된 소유 기기에서 신규 UI 검사의 변경 전 결과와 원문을 보존합니다."""
import datetime
import hashlib
import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path('D:/2026/PXE Project/15차/source')
ADB = 'D:/PXE15Build/android/android-sdk/platform-tools/adb.exe'
PREFIX = [ADB, '-P', '5043', '-s', 'emulator-5598']
EVIDENCE = ROOT / 'evidence'

def run(name, argv, timeout=30, expected_red=False):
    start = datetime.datetime.now().astimezone().isoformat()
    result = subprocess.run(argv, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    raw = result.stdout
    (EVIDENCE / (name + '.txt')).write_bytes(raw)
    output = raw.decode('utf-8', errors='replace')
    metadata = dict(kst_start=start, kst_end=datetime.datetime.now().astimezone().isoformat(),
                    session='01a131ad-d167-7873-9aa6-4d81ff131c60', task='pxe15-recognition-simplify-20261028',
                    argv=argv, cwd=str(ROOT), exit_code=result.returncode, log_bytes=len(raw),
                    log_sha256=hashlib.sha256(raw).hexdigest(), truncated=False, timeout_seconds=timeout,
                    product_changed=False, test_changed=True, repair_used=0, input_fix_used=2,
                    expected_red=expected_red, success_marker=bool(re.search(r'OK \(\d+ tests?\)', output)))
    (EVIDENCE / (name + '.json')).write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding='utf-8')
    print(name, 'exit', result.returncode, 'bytes', len(raw), flush=True)
    print(output[-5500:], flush=True)
    if result.returncode:
        raise SystemExit(result.returncode)
    return output

assert run('ui-red-owned-device', PREFIX + ['shell', 'getprop', 'ro.boot.qemu.avd_name']).strip() == 'PXE15'
for name, relative in [('ui-red-install-product', 'work/build/app/outputs/apk/release/app-release.apk'),
                       ('ui-red-install-tests', 'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk')]:
    assert 'Success' in run(name, PREFIX + ['install', '-r', str(ROOT / relative)], timeout=60)
run('ui-red-three', PREFIX + ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
    'kr.yunseok.touchmacro.PxeRecognitionSettingsTest',
    'kr.yunseok.touchmacro.test/androidx.test.runner.AndroidJUnitRunner'], timeout=60, expected_red=True)
