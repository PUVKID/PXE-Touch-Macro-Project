"""소유 시험 기기에서 같은 실제 OCR fixture의 이전/적응 실행을 각각 측정합니다."""
import json
import re
import sys
from check_regression import ROOT, PROJECT, run

mode=sys.argv[1] if len(sys.argv)==2 else ''
if mode not in ('baseline','adaptive'):
    raise SystemExit('baseline 또는 adaptive 한 가지를 지정하세요')
prefix=[str(PROJECT/'코드/NEXTools/android-sdk/platform-tools/adb.exe'),'-P','5043','-s','emulator-5598','shell']
if run(prefix+['getprop','ro.boot.qemu.avd_name'],timeout=10).strip()!='PXE16':
    raise SystemExit('소유 PXE16 시험 기기가 아닙니다')
version=15 if mode=='baseline' else 16
metadata=run(prefix+['dumpsys','package','kr.yunseok.touchmacro'],timeout=10)
if not re.search(r'versionCode='+str(version)+r'\s',metadata):
    raise SystemExit('성능 비교의 설치 버전이 다릅니다')
method='fifteenRoundsBaselineWithExplicitDelay' if mode=='baseline' else 'fifteenRoundsAdaptiveCompletesWithinTwoMinutes'
inner=230 if mode=='baseline' else 150
command=prefix+['am','instrument','-w','-r','-e','class','kr.yunseok.touchmacro.AdaptiveWaitBenchmarkTest#'+method,'kr.yunseok.touchmacro.test/androidx.test.runner.AndroidJUnitRunner']
print(json.dumps(dict(mode=mode,version=version,expected_tests=1,whole_macro_rounds=15,text_steps_per_round=2,expected_taps=30,argv=command,cwd=str(ROOT),timeout_seconds=inner),ensure_ascii=False),flush=True)
run(command,timeout=inner,require_success=True)
