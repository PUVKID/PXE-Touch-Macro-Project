"""소유 PXE14의 실제 UI 트리에서 좌표를 구해 수동 화면 검토를 돕습니다."""
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = 'D:/PXE14Build/android/android-sdk/platform-tools/adb.exe'
PREFIX = [ADB, '-P', '5043', '-s', 'emulator-5598']
OUT = ROOT / 'work/ui-final14-repair5'
OUT.mkdir(exist_ok=True)

def call(args, binary=False):
    result = subprocess.run(PREFIX + args, cwd=ROOT, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=20, shell=False)
    if result.returncode:
        raise RuntimeError(f'{args!r}: {result.returncode}: {result.stderr.decode("utf-8",errors="strict")}')
    if len(result.stdout) > 10 * 1024 * 1024:
        raise RuntimeError('UI 출력이 10MiB 상한을 넘었습니다')
    return result.stdout if binary else result.stdout.decode('utf-8',errors='strict')

if call(['shell','getprop','ro.boot.qemu.avd_name']).strip() != 'PXE14':
    raise SystemExit('소유 PXE14가 아닙니다')

def tree():
    call(['shell','uiautomator','dump','/sdcard/pxe14-review.xml'])
    return ET.fromstring(call(['shell','cat','/sdcard/pxe14-review.xml']))

def bounds(node):
    values = list(map(int,re.findall(r'-?\d+',node.get('bounds',''))))
    return values if len(values)==4 else [0,0,0,0]

def swipe(down):
    nodes=list(tree().iter('node'))
    sc=next(n for n in nodes if n.get('scrollable')=='true')
    left,top,right,bottom=bounds(sc)
    x=(left+right)//2
    start,end=(bottom-90,top+90) if down else (top+90,bottom-90)
    call(['shell','input','swipe',str(x),str(start),str(x),str(end),'300'])
    time.sleep(.3)

def click(value,description=False):
    key='content-desc' if description else 'text'
    for attempt in range(4):
        nodes=list(tree().iter('node'))
        found=next((n for n in nodes if n.get(key)==value and n.get('enabled')=='true' and bounds(n)[3]>bounds(n)[1]),None)
        if found is not None:
            left,top,right,bottom=bounds(found)
            call(['shell','input','tap',str((left+right)//2),str((top+bottom)//2)])
            time.sleep(.3)
            return
        if attempt<3:
            swipe(True)
    raise RuntimeError(f'실제 UI에서 찾지 못했습니다: {key}={value!r}')

action=sys.argv[1]
if action=='dump':
    for node in list(tree().iter('node'))[:150]:
        if node.get('text') or node.get('content-desc'):
            print(node.get('text'), '|',node.get('content-desc'),'|',node.get('bounds'))
elif action=='capture':
    image=call(['exec-out','screencap','-p'],True)
    if not image.startswith(b'\x89PNG\r\n\x1a\n'):
        raise RuntimeError('PNG 원본이 아닙니다')
    path=OUT/(sys.argv[2]+'.png')
    path.write_bytes(image)
    print(str(path),len(image))
elif action=='tap':
    click(sys.argv[2])
elif action=='desc':
    click(sys.argv[2],True)
elif action=='down':
    swipe(True)
elif action=='up':
    swipe(False)
elif action=='back':
    call(['shell','input','keyevent','4'])
    time.sleep(.3)
elif action=='settings':
    print(call(['shell','am','start','-n','kr.yunseok.touchmacro/.MainActivity','--ez','settings','true']))
else:
    raise SystemExit('지원하지 않는 검토 동작')
