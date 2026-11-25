"""전용 27차 검사 장치만 확인하고 지정한 검사를 실행합니다."""
from pathlib import Path
import subprocess,sys,re
from project_paths import SDK
adb=str(SDK/'platform-tools/adb.exe')
prefix=[adb,'-P','5047','-s','emulator-5600']
def run(args,timeout=30):
 r=subprocess.run(prefix+args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0),check=True)
 return r.stdout.decode('utf-8','strict')
if run(['shell','getprop','ro.boot.qemu.avd_name']).strip()!='PXE26Runtime':raise SystemExit('전용 검사 장치가 아닙니다')
if not re.search(r'versionCode=27\s',run(['shell','dumpsys','package','kr.yunseok.touchmacro'])):raise SystemExit('설치 버전이 27차가 아닙니다')
classes=sys.argv[1:]
if not classes or any(not re.fullmatch(r'[A-Za-z0-9]+Test(?:#[A-Za-z0-9]+)?',x) for x in classes):raise SystemExit('검사 클래스 이름을 지정하세요')
output=run(['shell','am','instrument','-w','-r','-e','class',','.join('kr.yunseok.touchmacro.'+x for x in classes),'kr.yunseok.touchmacro.test/androidx.test.runner.AndroidJUnitRunner'],600)
print(output)
if not re.search(r'OK \(\d+ tests?\)',output) or 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output:raise SystemExit(1)
