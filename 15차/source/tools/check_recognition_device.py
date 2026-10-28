"""소유 PXE15에서 신규 집중 검사를 승인된 내부 시간으로로 제한해 실행합니다."""
import re
import sys
import time
import json
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from check_regression import PROJECT, ROOT, run

started = time.monotonic()
adb = str(PROJECT / "코드/NEXTools/android-sdk/platform-tools/adb.exe")
prefix = [adb, "-P", "5043", "-s", "emulator-5598", "shell"]
device = run(prefix + ["getprop", "ro.boot.qemu.avd_name"], timeout=10).strip()
if device != "PXE15":
    raise SystemExit("소유 PXE15가 아니므로 검사를 중단합니다")
package = run(prefix + ["dumpsys", "package", "kr.yunseok.touchmacro"], timeout=10)
if not re.search(r"versionCode=15\s", package):
    raise SystemExit("설치 버전이 15가 아닙니다")
classes = sys.argv[1:]
if not classes or any(not re.fullmatch(r"(?:Recognition(?:Mode|Region)|PxeRecognitionSettings)Test(?:#[A-Za-z]+)?", item) for item in classes):
    raise SystemExit("승인된 신규 인식 검사만 실행합니다")
inner=60 if all(item.startswith('PxeRecognitionSettingsTest') for item in classes) else 120
outer=90 if inner==60 else 150
command = prefix + ["am", "instrument", "-w", "-r", "-e", "class",
                    ",".join("kr.yunseok.touchmacro." + item for item in classes),
                    "kr.yunseok.touchmacro.test/androidx.test.runner.AndroidJUnitRunner"]
print(json.dumps({"argv": command, "cwd": str(ROOT), "instrument_timeout_seconds": inner,
                  "outer_budget_seconds": outer}, ensure_ascii=False), flush=True)
run(command, timeout=min(inner, max(1, outer-5-(time.monotonic()-started))), require_success=True)
