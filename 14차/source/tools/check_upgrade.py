"""소유 PXE14 장치에서만 13→14 실제 업데이트와 기존 데이터 보존을 검사합니다."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import sys
sys.dont_write_bytecode = True
from check_regression import ROOT, run

BRIDGE = Path('D:/PXE14Build')
ADB = str(BRIDGE / 'android/android-sdk/platform-tools/adb.exe')
PREFIX = [ADB, '-P', '5043', '-s', 'emulator-5598']
PACKAGE = 'kr.yunseok.touchmacro'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    device = run(PREFIX + ['shell', 'getprop', 'ro.boot.qemu.avd_name']).strip()
    if device != 'PXE14':
        raise SystemExit('소유한 PXE14 장치가 아닙니다')
    delivery = ROOT.parent
    baseline = json.loads((delivery / '13차_보존기준.json').read_text(encoding='utf-8-sig'))
    previous = Path('D:/2026/PXE Project/13차/PXE_13차_v1.3_PC용.apk')
    entry = next(item for item in baseline if Path(item['path']) == previous)
    if previous.stat().st_size != entry['bytes'] or sha(previous).upper() != entry['sha256']:
        raise SystemExit('13차 원본 APK 보존 기준 불일치')
    current = delivery / 'PXE_14차_v1.4_PC용.apk'
    tests = ROOT / 'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
    work = ROOT / 'work/upgrade14'
    work.mkdir(parents=True, exist_ok=True)
    for source, name in [(previous, 'previous.apk'), (current, 'current.apk'), (tests, 'tests.apk')]:
        target = work / name
        shutil.copyfile(source, target)
        if sha(source) != sha(target):
            raise SystemExit('설치용 ASCII 복사본 해시 불일치')
        print(f'{name} SHA256 {sha(source)}')
    metadata = run(PREFIX + ['shell', 'dumpsys', 'package', PACKAGE])
    if re.search(r'versionCode=14\s', metadata):
        # 독립 재실행 때에도 같은 전용 시험 앱만 초기화합니다.
        run(PREFIX + ['uninstall', PACKAGE], timeout=40)
    run(PREFIX + ['install', '-r', str(BRIDGE / 'source/work/upgrade14/previous.apk')], timeout=60)
    metadata = run(PREFIX + ['shell', 'dumpsys', 'package', PACKAGE])
    if not re.search(r'versionCode=13\s', metadata):
        raise SystemExit('업데이트 전 설치 버전이 13차가 아닙니다')
    run(PREFIX + ['install', '-r', str(BRIDGE / 'source/work/upgrade14/tests.apk')], timeout=60)
    instrument = PREFIX + ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class']
    runner = PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner'
    run(instrument + [PACKAGE + '.PxeUpgradeTest#seedPreviousVersion', runner], timeout=60, require_success=True)
    run(PREFIX + ['install', '-r', str(BRIDGE / 'source/work/upgrade14/current.apk')], timeout=60)
    metadata = run(PREFIX + ['shell', 'dumpsys', 'package', PACKAGE])
    if not re.search(r'versionCode=14\s', metadata):
        raise SystemExit('업데이트 후 설치 버전이 14차가 아닙니다')
    run(instrument + [PACKAGE + '.PxeUpgradeTest#verifyAfterPxeUpdate', runner], timeout=60, require_success=True)
    print('13→14 실제 업데이트와 기존 저장 데이터 보존: 2개 통과')

if __name__ == '__main__':
    main()
