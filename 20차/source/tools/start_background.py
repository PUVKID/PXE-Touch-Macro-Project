"""현재 사용자의 소유 장기 시험을 숨김 WMI 프로세스로 시작하고 수명을 직접 확인합니다."""
import argparse,base64,ctypes,json,os,re,subprocess,sys
from ctypes import wintypes
from project_paths import *
from soak_contract import atomic_json

parser=argparse.ArgumentParser();parser.add_argument('kind',choices=('emulator','preflight','main'));parser.add_argument('--run-id');arguments=parser.parse_args()
if arguments.run_id and not re.fullmatch('[A-Za-z0-9_-]+',arguments.run_id):raise ValueError('실행 ID 형식 오류')
if arguments.kind=='emulator':argv=[sys.executable,'-B','-X','utf8',str(ROOT/'tools/launch_emulator.py')];tag=arguments.run_id or 'emulator-supervisor'
else:
    if not arguments.run_id:raise ValueError('고유 run-id 필수')
    argv=[sys.executable,'-B','-X','utf8',str(ROOT/'tools/run_soak.py'),'--seconds','300' if arguments.kind=='preflight' else '86400','--run-id',arguments.run_id];tag=arguments.run_id
output=CONTROL/(tag+'-host-output.txt');record_path=CONTROL/(tag+'-background.json')
with output.open('xb'):pass
# WMI는 표준 출력 핸들을 받지 않으므로 같은 사용자 Python이 자기 출력만 연결합니다.
wrapper='import os,sys,runpy;stream=open('+repr(str(output))+',"a",encoding="utf-8",buffering=1);sys.stdout=stream;sys.stderr=stream;'
if arguments.kind=='emulator':wrapper+='os.environ["PXE20_EMULATOR_TAG"]='+repr(tag)+';'
wrapper+='sys.argv='+repr(argv[4:])+';sys.path.insert(0,'+repr(str(Path(argv[4]).parent))+');runpy.run_path(sys.argv[0],run_name="__main__")'
actual_argv=[sys.executable,'-B','-X','utf8','-c',wrapper]
flags=subprocess.CREATE_NO_WINDOW|subprocess.CREATE_NEW_PROCESS_GROUP|subprocess.CREATE_BREAKAWAY_FROM_JOB
payload=base64.b64encode(json.dumps({'command_line':subprocess.list2cmdline(actual_argv),'cwd':str(ROOT),'flags':flags},ensure_ascii=False).encode('utf-8')).decode('ascii')
script="$ErrorActionPreference='Stop';$payload=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('"+payload+"'))|ConvertFrom-Json;"
script+="$startup=New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[UInt16]0;CreateFlags=[UInt32]$payload.flags};"
script+="$created=Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine=$payload.command_line;CurrentDirectory=$payload.cwd;ProcessStartupInformation=$startup};"
script+="if($created.ReturnValue -ne 0){throw ('WMI Create ReturnValue='+$created.ReturnValue)};"
script+="$spawned=Get-CimInstance Win32_Process -Filter ('ProcessId='+$created.ProcessId);$owner=$null;if($null -ne $spawned){$owner=Invoke-CimMethod -InputObject $spawned -MethodName GetOwnerSid};"
script+="[pscustomobject]@{pid=$created.ProcessId;return_value=$created.ReturnValue;child_observed=($null -ne $spawned);owner_return_value=$owner.ReturnValue;child_sid=$owner.Sid;current_sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value}|ConvertTo-Json -Compress"
powershell=Path(os.environ.get('SystemRoot','C:/Windows'))/'System32/WindowsPowerShell/v1.0/powershell.exe'
request=[str(powershell),'-NoProfile','-NonInteractive','-Command',script]
record={'argv':argv,'process_argv':actual_argv,'wmi_argv':request,'cwd':str(ROOT),'visible_window':False,
        'stdout':str(output),'kind':arguments.kind,'creationflags':flags,'backend':'local_wmi','state':'CREATING'}
atomic_json(record_path,record)
result=subprocess.run(request,cwd=ROOT,shell=False,capture_output=True,timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
record.update(wmi_exit_code=result.returncode,wmi_stdout_base64=base64.b64encode(result.stdout).decode('ascii'),wmi_stderr_base64=base64.b64encode(result.stderr).decode('ascii'))
atomic_json(record_path,record)
if result.returncode:raise RuntimeError('현재 사용자 WMI 기동 실패: '+result.stderr.decode('utf-8','backslashreplace'))
created=json.loads(result.stdout.decode('utf-8-sig'));record.update(created);atomic_json(record_path,record)
if not record['child_observed']:
    record.update(state='FAILED',error='WMI 생성 뒤 자식이 조회 전에 종료했습니다');atomic_json(record_path,record)
    raise RuntimeError(record['error']+'; 원문: '+str(output))
kernel=ctypes.WinDLL('kernel32',use_last_error=True)
kernel.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];kernel.OpenProcess.restype=wintypes.HANDLE
kernel.IsProcessInJob.argtypes=[wintypes.HANDLE,wintypes.HANDLE,ctypes.POINTER(wintypes.BOOL)]
kernel.TerminateProcess.argtypes=[wintypes.HANDLE,wintypes.UINT];kernel.WaitForSingleObject.argtypes=[wintypes.HANDLE,wintypes.DWORD]
kernel.CloseHandle.argtypes=[wintypes.HANDLE]
handle=kernel.OpenProcess(0x101001,False,record['pid'])
if not handle:
    record.update(state='FAILED',open_process_error=ctypes.get_last_error());atomic_json(record_path,record)
    raise OSError(record['open_process_error'],'소유 장기 프로세스 조회 핸들 없음')
try:
    inside=wintypes.BOOL();checked=kernel.IsProcessInJob(handle,None,ctypes.byref(inside))
    record.update(job_check_succeeded=bool(checked),child_in_job=bool(inside.value),job_check_error=ctypes.get_last_error() if not checked else 0,
                  owner_matches=record['owner_return_value']==0 and record['child_sid']==record['current_sid'])
    atomic_json(record_path,record)
    if not checked or inside.value or not record['owner_matches']:
        kernel.TerminateProcess(handle,1);record['cleanup_wait_result']=kernel.WaitForSingleObject(handle,5000)
        record['state']='FAILED';atomic_json(record_path,record)
        raise RuntimeError('장기 시험의 동일 사용자 또는 작업 그룹 분리 확인 실패: '+json.dumps(record,ensure_ascii=False))
    record['state']='CREATED';atomic_json(record_path,record)
finally:kernel.CloseHandle(handle)
print({key:record[key] for key in ('pid','cwd','kind','backend','state','owner_matches','job_check_succeeded','child_in_job')})
