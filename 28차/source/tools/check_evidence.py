"""실제 검사 원문·파일 지문·장치 연속 기록을 읽기 전용으로 대조합니다."""
from pathlib import Path
import argparse,json,hashlib,re,xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--evidence-dir',type=Path,required=True);p.add_argument('--build-dir',type=Path,required=True);a=p.parse_args();w=a.evidence_dir;b=a.build_dir
meta=json.loads((w/'연속운용_status.json').read_text('utf-8'));assert meta['state']=='PASS';assert meta['boot_id']==meta['end_boot_id'];assert meta['bluestacks'] is False
apk=b/'work/build/app/outputs/apk/release/app-release.apk';test=b/'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk'
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
assert sha(apk)==meta['product_sha256'];assert sha(test)==meta['test_apk_sha256'];assert sha(w/'검사소스/kr/yunseok/touchmacro/Endurance27Test.java')==meta['test_source_sha256']
inputs=json.loads((w/'제품입력_정본대조.json').read_text('utf-8'))['files'];canonical=b.parents[2]/'27차/source'
assert len(inputs)==116 and {f.relative_to(canonical).as_posix() for f in (canonical/'app').rglob('*') if f.is_file()}=={r['path'] for r in inputs}
assert all(sha(canonical/r['path'])==r['sha256']==sha(b/r['path']) and (canonical/r['path']).stat().st_size==r['bytes'] for r in inputs)
lines=(w/'장치_연속운용.jsonl').read_text('utf-8').split('\n')
if lines[-1]=='':lines.pop()
assert lines and all(lines);rows=[json.loads(line) for line in lines];assert [r['cycle'] for r in rows]==list(range(1,len(rows)+1));assert len({r['pid'] for r in rows})==1
assert rows[0]['elapsedMs']<=120000 and all(rows[i]['elapsedMs']-rows[i-1]['elapsedMs']<=120000 for i in range(1,len(rows)));assert rows[-1]['elapsedMs']>=3600000;assert all(rows[i]['elapsedMs']>rows[i-1]['elapsedMs'] for i in range(1,len(rows)));assert all(r['taps']==r['cycle']*15 and r['entries']==r['cycle'] and r['backs']==r['cycle'] and 0<=r['pendingCapture']<=2 for r in rows)
host_lines=(w/'호스트_관측.jsonl').read_text('utf-8').split('\n')
if host_lines[-1]=='':host_lines.pop()
assert host_lines and all(host_lines);host=[json.loads(line) for line in host_lines];assert len(host)>=55;assert len({r['observer_pid'] for r in host})==1;assert all(r['run_id']==meta['run_id'] and r['boot_matches'] and r['boot_id']==meta['boot_id'] and r.get('last_cycle',{}).get('pid')==rows[-1]['pid'] for r in host)
from datetime import datetime
start=datetime.fromisoformat(meta['at']);end=datetime.fromisoformat(meta['finished_at']);first_host=datetime.fromisoformat(host[0]['at']);last_host=datetime.fromisoformat(host[-1]['at'])
assert 0<=(first_host-start).total_seconds()<=120 and 0<=(end-last_host).total_seconds()<=120
assert (last_host-first_host).total_seconds()>=meta['elapsed_ms']/1000-240
assert all(meta[k]==rows[-1][v] for k,v in [('cycles','cycle'),('taps','taps'),('elapsed_ms','elapsedMs'),('device_pid','pid')])
assert all(0<(datetime.fromisoformat(host[i]['at'])-datetime.fromisoformat(host[i-1]['at'])).total_seconds()<=120 for i in range(1,len(host)))
checks=[('27차_속도비교',1,'Sustained26Test',['fiveBlocksOfFifteenRealOcrTapsThenStopWhileMissing']),('27차_캡처복구',3,'CaptureRecoveryTest',['twoIntervalFailuresRecoverAndFreshOcrConfirmationPrecedesOneEntry','stopDuringRetryDoesNotCaptureOrEnterLater','persistentIntervalErrorKeepsSafeRecoveryUntilUserStops']),('27차_화면과갱신_수리후',1,'Ui27Test',['lightDarkNavigationAndStatusUpdates']),('27차_60분_실제연속운용',1,'Endurance27Test',['oneHourRealOcrEntryRepeatBackRefreshWithoutRestart'])]
for label,count,classname,methods in checks:
 text=(w/(label+'.bin')).read_bytes().decode('utf-8');command=json.loads((w/(label+'.json')).read_text('utf-8'))
 selected='kr.yunseok.touchmacro.'+classname+('#'+methods[0] if label in ['27차_속도비교','27차_60분_실제연속운용'] else '')
 assert command['state']=='PASS' and command['exit_code']==0 and command['argv'][-2]==selected
 assert re.findall(r'^OK \((\d+) tests?\)\s*$',text,re.M)==[str(count)]
 assert not any(x in text for x in ['FAILURES!!!','INSTRUMENTATION_FAILED'])
 assert set(re.findall(r'^INSTRUMENTATION_STATUS: numtests=(\d+)\s*$',text,re.M))=={str(count)}
 assert set(re.findall(r'^INSTRUMENTATION_STATUS: class=(\S+)\s*$',text,re.M))=={'kr.yunseok.touchmacro.'+classname}
 assert set(re.findall(r'^INSTRUMENTATION_STATUS: test=(\S+)\s*$',text,re.M))==set(methods)
 codes=re.findall(r'^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$',text,re.M)
 assert set(codes)=={'0','1'} and codes.count('0')==count and codes.count('1')==count

results=[ET.parse(f).getroot() for f in (b/'work/build/app/test-results/testDebugUnitTest').glob('TEST-*.xml')];assert sum(int(x.get('tests',0)) for x in results)==106;assert all(int(x.get('failures',0))+int(x.get('errors',0))+int(x.get('skipped',0))==0 for x in results);assert not ET.parse(b/'work/build/app/reports/lint-results-debug.xml').getroot().findall('issue')
for label in ['27_light.png','27_dark.png','27_light_settings.png','27_dark_settings.png','27_light_text.png','27_dark_text.png']:assert (w/label).read_bytes().startswith(bytes([137,80,78,71,13,10,26,10]))
assert (w/'오류_전.txt').read_bytes()==(w/'오류_후.txt').read_bytes(),'연속 검사 전후 오류 기록 변경: 원문 조사 필요'
print('실제 실행 근거·106단위·린트·6화면·장치 60분 기록 대조 통과:',len(rows),'바퀴',rows[-1]['taps'],'터치')
