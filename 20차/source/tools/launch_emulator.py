"""기존 소유 AVD만 비가시 프로세스로 기동하고 제한된 원문을 수집합니다."""
import os,subprocess,sys,time,re
from project_paths import *
from soak_contract import RotatingBytes,atomic_json

tag=os.environ.get('PXE20_EMULATOR_TAG','emulator-supervisor')
if not re.fullmatch('[A-Za-z0-9_-]+',tag):raise ValueError('기동 기록 ID 형식 오류')
folder=CONTROL/tag;folder.mkdir(parents=True,exist_ok=False)
emulator=PROJECT/'work/build19/emulator/emulator.exe'
home=PROJECT/'work/build19/source/work/android-user'
argv=[str(emulator),'-avd','PXE19','-port','5598','-no-window','-no-audio','-no-snapshot','-gpu','swiftshader','-feature','-Vulkan','-memory','2048','-cores','4']
environment=os.environ.copy();environment.update(ANDROID_USER_HOME=str(home),ANDROID_AVD_HOME=str(home/'avd'),ANDROID_EMULATOR_HOME=str(home),
    ANDROID_HOME=str(SDK),ANDROID_SDK_ROOT=str(SDK),ANDROID_ADB_SERVER_PORT='5043',ADB_SERVER_SOCKET='tcp:localhost:5043')
assert emulator.is_file() and (home/'avd/PXE19.avd/config.ini').is_file()
assert not home.is_junction() and not home.is_symlink()
probe=subprocess.run(PREFIX+['get-state'],cwd=BUILD,shell=False,capture_output=True,timeout=10,creationflags=subprocess.CREATE_NO_WINDOW)
if probe.returncode==0:raise RuntimeError('소유 serial이 이미 실행 중이므로 중복 기동하지 않습니다')
process=subprocess.Popen(argv,cwd=BUILD,shell=False,env=environment,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
state={'supervisor_pid':os.getpid(),'emulator_pid':process.pid,'argv':argv,'cwd':str(BUILD),'avd_home':str(home/'avd'),'specific_serial':'emulator-5598','adb_port':5043,'new_links':0,
       'raw_path':str(folder/'emulator.bin'),'launch_record_path':str(folder/'launch.json'),
       'absence_probe':{'exit_code':probe.returncode,'stdout':probe.stdout.decode('utf-8','backslashreplace'),'stderr':probe.stderr.decode('utf-8','backslashreplace')}}
atomic_json(folder/'launch.json',state)
atomic_json(CONTROL/'emulator-launch.json',state)
log=RotatingBytes(folder/'emulator.bin')
while True:
    block=process.stdout.read(4096)
    if not block:break
    log.write(block)
state['exit_code']=process.wait();atomic_json(folder/'exit.json',state)
