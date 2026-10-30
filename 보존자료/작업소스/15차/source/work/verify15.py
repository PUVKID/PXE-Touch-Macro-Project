"""동결된 명세를 바꾸지 않고 제한 시간 안에서 독립 검사 결과를 수집합니다."""
import json,sys,time,datetime
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(Path.home()/'.codex/hooks'))
from routing_stop import run_check
label=sys.argv[1]
plan=json.loads((ROOT/'tools/required-check-plan.json').read_text(encoding='utf-8'))
start=time.monotonic()
results=[]
out=ROOT/'evidence'/('필수검사_'+label+'.json')
for item in plan['checks']:
    remaining=plan['total_timeout_seconds']-(time.monotonic()-start)
    check={'id':item['id'],'argv':[sys.executable if x=='{python}' else x for x in item['argv']], 'cwd':str(ROOT/item['cwd']),'timeout_seconds':min(item['timeout_seconds'],max(.001,remaining))}
    result=run_check(check)
    results.append(result)
    document={'label':label,'at_kst':datetime.datetime.now().astimezone().isoformat(),'total_limit_seconds':plan['total_timeout_seconds'],'elapsed_seconds':time.monotonic()-start,'results':results}
    out.write_text(json.dumps(document,ensure_ascii=False,indent=2),encoding='utf-8')
    print(item['id'],result['status'],round(result['elapsed_seconds'],2) if 'elapsed_seconds' in result else '',flush=True)
    if result['status']!='passed':
        print(json.dumps(result,ensure_ascii=False),flush=True)
        raise SystemExit(1)
small=json.loads((ROOT/'tools/recognition-check-plan.json').read_text(encoding='utf-8'))['small_screen']
sys.path.insert(0,str(ROOT/'tools'))
from check_regression import run
adb=str(ROOT.parents[1]/'코드/NEXTools/android-sdk/platform-tools/adb.exe')
prefix=[adb,'-P','5043','-s','emulator-5598','shell']
if run(prefix+['getprop','ro.boot.qemu.avd_name']).strip()!='PXE15':raise SystemExit('소유장치불일치')
try:
    run(prefix+['wm','size',small['size']]);run(prefix+['wm','density',str(small['density'])])
    remaining=plan['total_timeout_seconds']-(time.monotonic()-start)
    check={'id':'320dp 작은 화면 UI6','argv':[sys.executable,'-B','-X','utf8','tools/check_device.py',*small['classes']], 'cwd':str(ROOT),'timeout_seconds':min(small['timeout_seconds'],max(.001,remaining))}
    result=run_check(check);results.append(result)
    document={'label':label,'at_kst':datetime.datetime.now().astimezone().isoformat(),'total_limit_seconds':plan['total_timeout_seconds'],'elapsed_seconds':time.monotonic()-start,'results':results}
    out.write_text(json.dumps(document,ensure_ascii=False,indent=2),encoding='utf-8')
    print(check['id'],result['status'],flush=True)
    if result['status']!='passed':print(json.dumps(result,ensure_ascii=False),flush=True);raise SystemExit(1)
finally:
    run(prefix+['wm','size',small['restore']['size']]);run(prefix+['wm','density',str(small['restore']['density'])])
print('필수 검사 모두 통과',round(time.monotonic()-start,3),flush=True)
