"""소유한 PXE13 검사 장치와 13차 설치를 확인한 뒤 기기 회귀 검사를 실행합니다."""
import re
import sys
sys.dont_write_bytecode = True
from check_regression import PROJECT, run

adb = str(PROJECT / "코드/NEXTools/android-sdk/platform-tools/adb.exe")
prefix = [adb, "-P", "5043", "-s", "emulator-5598", "shell"]
device = run(prefix + ["getprop", "ro.boot.qemu.avd_name"]).strip()
if device != "PXE13":
    raise SystemExit("소유한 PXE13 검사 장치가 아닙니다")
package = run(prefix + ["dumpsys", "package", "kr.yunseok.touchmacro"])
if not re.search(r"versionCode=13\s", package):
    raise SystemExit("설치 버전이 13차가 아닙니다")
classes = sys.argv[1:] or ["OcrStallRecoveryTest", "ContinuousOcrTest", "ScreenDelayRecoveryTest", "ReaderResponsivenessTest"]
if any(not re.fullmatch(r"[A-Za-z]+Test(?:#[A-Za-z]+)?", item) for item in classes):
    raise SystemExit("검사 클래스 또는 메서드 형식 오류")
run(prefix + ["am", "instrument", "-w", "-r", "-e", "class",
              ",".join("kr.yunseok.touchmacro." + item for item in classes),
              "kr.yunseok.touchmacro.test/androidx.test.runner.AndroidJUnitRunner"],
    timeout=240, require_success=True)
