"""실제 소유 장치 확인 뒤 고정 제품과 새 시험 APK만 설치합니다."""
import hashlib,json,re,subprocess,time
from project_paths import *
from soak_contract import atomic_json

rows=[]
def execute(argv,timeout=20):
    result=subprocess.run(argv,cwd=BUILD,shell=False,capture_output=True,timeout=timeout,creationflags=subprocess.CREATE_NO_WINDOW)
    rows.append({'argv':argv,'cwd':str(BUILD),'exit_code':result.returncode,'stdout':result.stdout.decode('utf-8','backslashreplace'),'stderr':result.stderr.decode('utf-8','backslashreplace')})
    atomic_json(CONTROL/'setup-device-commands.json',rows)
    return result

until=time.monotonic()+120
while True:
    boot=execute(PREFIX+['shell','getprop','sys.boot_completed'])
    if boot.returncode==0 and boot.stdout.strip()==b'1':break
    if time.monotonic()>=until:raise TimeoutError('PXE19 부팅 120초 상한')
    time.sleep(2)
for command,expected in [(['getprop','ro.boot.qemu.avd_name'],'PXE19'),(['getprop','ro.build.version.sdk'],'31')]:
    result=execute(PREFIX+['shell',*command]);assert result.returncode==0 and result.stdout.decode().strip()==expected
for command,expected in [(['wm','size'],'1080x2280'),(['wm','density'],'420'),(['cat','/sys/devices/system/cpu/online'],'0-3')]:
    result=execute(PREFIX+['shell',*command]);assert result.returncode==0 and expected in result.stdout.decode()
test=BUILD/'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
verify=execute([str(JDK/'bin/java.exe'),'-Dfile.encoding=UTF-8','-jar',str(SDK/'build-tools/35.0.0/lib/apksigner.jar'),'verify','--print-certs',str(test)])
assert verify.returncode==0
digest=re.search(rb'certificate SHA-256 digest: ([a-fA-F0-9]+)',verify.stdout).group(1).decode().lower()
assert digest=='57339c32b3661e18f18a7f9f7a08c539130211ab93ab0000812f99c949c5ad3e'
assert hashlib.sha256((BUILD/'work/target19.apk').read_bytes()).hexdigest()==PC_SHA
for path in (BUILD/'work/target19.apk',test):
    result=execute(PREFIX+['install','-r',str(path)],timeout=60)
    assert result.returncode==0 and b'Success' in result.stdout
atomic_json(CONTROL/'device-ready.json',{'serial':'emulator-5598','api':31,'product_sha256':PC_SHA,
           'test_apk':str(test),'test_sha256':hashlib.sha256(test.read_bytes()).hexdigest(),'test_certificate_sha256':digest})
print('실제 PXE19 Android31 확인·고정19제품/새검사APK 설치·검사서명 PASS')
