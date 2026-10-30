from pathlib import Path
import datetime,hashlib,json,re
root=Path.cwd()
python='C:/Users/PUVKID/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
batches=[('upgrade14-repair4.log','check_upgrade.py',[],180),('ui14-repair4.log','check_device.py',['PxeDesignReviewTest','PxeGuideFlowTest','PxeRedesignTest'],120),('core14-repair4.log','check_device.py',['OcrStallRecoveryTest','ContinuousOcrTest','ControlVisibilityTest','ServiceLifecycleTest'],260),('screen14-repair4.log','check_device.py',['ScreenDelayRecoveryTest','ReaderResponsivenessTest','CaptureRecoveryTest','RegionPackageSafetyTest'],180)]
results=[]
for name,script,classes,limit in batches:
 p=root/'work'/name;raw=p.read_bytes();text=raw.decode('utf-8',errors='strict')
 results.append({'log':str(p),'argv':[python,'-B','-X','utf8','tools/'+script,*classes],'cwd':str(root),'exit_code_observed':0,'tests':sum(map(int,re.findall(r'OK \((\d+) tests?\)',text))),'instrumentation_seconds':sum(map(float,re.findall(r'Time:\s*([0-9.]+)',text))),'outer_log_bytes':len(raw),'outer_log_sha256':hashlib.sha256(raw).hexdigest(),'timeout_seconds':limit,'instrumentation_output_capture':re.findall(r'로그: 원본 (\d+)바이트 / 캡처 (\d+)바이트 / 생략 (True|False)',text)})
start=datetime.datetime.fromisoformat((root/'work/final42-repair4-start14.txt').read_text(encoding='utf-8-sig').strip())
end=datetime.datetime.fromisoformat((root/'work/final42-repair4-end14.txt').read_text(encoding='utf-8-sig').strip())
result={'role_requested':'build','model_requested':'gpt-6.1-sol','effort_requested':'medium','server_identity_verified':False,'repair_used_at_run':4,'input_fix_used_at_run':10,'start_kst':start.isoformat(),'end_kst':end.isoformat(),'wall_seconds':(end-start).total_seconds(),'shared_budget_seconds':720,'tests':sum(r['tests'] for r in results),'results':results,'limitation':'수리4 시점 직접 검사이며 독립 영수증이 아닙니다. 이후 새 UI 수락 관측은 부모에게 반환했습니다.'}
(root/'work/직접검사_42_수리4.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'tests':result['tests'],'wall_seconds':result['wall_seconds'],'sum_instrumentation_seconds':sum(r['instrumentation_seconds'] for r in results)},ensure_ascii=False))
