from pathlib import Path
import datetime,hashlib,json,xml.etree.ElementTree as E
root=Path.cwd();old=Path('D:/2026/PXE Project/13차/source')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
ui_names={'Ui.java','MainActivity.java','FloatingController.java','SetupWizard.java','SetupCoach.java','RunHud.java','RegionPicker.java'}
java=root/'app/src/main/java/kr/yunseok/touchmacro'
protected=[{'file':p.name,'sha256':sha(p),'same_as_13':p.read_bytes()==(old/p.relative_to(root)).read_bytes()} for p in sorted(java.glob('*.java')) if p.name not in ui_names]
files=[p for p in sorted(root.rglob('*')) if p.is_file() and p.relative_to(root).parts[0] not in {'work','evidence','.codex'}]
entries=[{'path':p.relative_to(root).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p)} for p in files]
canonical=json.dumps(entries,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf-8')
xmls=list((root/'work/build/app/test-results/testDebugUnitTest').glob('*.xml'))
unit={'classes':len(xmls),'tests':sum(int(E.parse(p).getroot().get('tests','0')) for p in xmls),'failures':sum(int(E.parse(p).getroot().get('failures','0')) for p in xmls),'errors':sum(int(E.parse(p).getroot().get('errors','0')) for p in xmls)}
images=[]
for folder in ['ui-final14','ui-final14-repair5','ui-captures14-repair5/files']:
 for p in sorted((root/'work'/folder).glob('*')):
  if p.is_file():images.append({'path':p.relative_to(root).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p),'historical_diagnostic':p.name.startswith('long14_') or p.name in {'PXE14_긴문구_즉시.png','PXE14_긴문구_300ms후.png'}})
apks=[root.parent/'PXE_14차_v1.4_PC용.apk',root.parent/'PXE_14차_v1.4_노트10플러스.apk']
record={'created_kst':datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat(),'scope':'후속 OCR 제보 조사 전 UI 단계 동결. 전체 최종 승인 및 독립 영수증이 아닙니다.','session':'01a131ad-d167-7873-9aa6-4d81ff131c60','task':'pxe14-redesign-20261026','child':'/root/pxe14_build','role_requested':'build','model_requested':'gpt-6.1-sol','effort_requested':'medium','server_identity_verified':False,'repair_used':5,'input_fix_used':11,'verification_failures':0,'root':str(root),'global_agents_sha256':sha(Path('C:/Users/PUVKID/.codex/AGENTS.md')),'ui_modules':[e for e in entries if Path(e['path']).name in ui_names],'protected_non_ui':protected,'source_files_excluding_metadata':entries,'source_bundle_sha256':hashlib.sha256(canonical).hexdigest(),'source_file_count':len(entries),'snapshot_definition':'상대경로/바이트/SHA 배열의 정렬 JSON. work/evidence/.codex 제외. 중앙 영수증의 sourceSnapshot과 동일하다고 주장하지 않습니다.','previous_routing_prefix_preserved':(root/'ROUTING-LESSONS.md').read_bytes().startswith((old/'ROUTING-LESSONS.md').read_bytes()),'unit':unit,'lint_issues':len(E.parse(root/'work/build/app/reports/lint-results-debug.xml').getroot().findall('issue')),'apk':[{'path':str(p),'bytes':p.stat().st_size,'sha256':sha(p)} for p in apks],'captures':images,'device':{'name':'PXE14','serial':'emulator-5598','adb_server':5043,'android':'12','width':1080,'height':2280,'density':420,'returned':True},'next_action':'부모가 새 사용자 OCR 제보를 별도 deep에 배정해 후속 원인 조사/새 동결/독립 검증을 조율합니다.'}
(root/'work/UI단계_동결_수리5.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
(root/'work/캡처_목록_수리5.json').write_text(json.dumps(images,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'source_files':len(entries),'source_bundle_sha256':record['source_bundle_sha256'],'protected':len(protected),'protected_changed':[v['file'] for v in protected if not v['same_as_13']],'unit':unit,'lint_issues':record['lint_issues'],'previous_routing_prefix_preserved':record['previous_routing_prefix_preserved'],'manual_previous':len(list((root/'work/ui-final14').glob('*.png'))),'manual_current':len(list((root/'work/ui-final14-repair5').glob('*.png')))},ensure_ascii=False))
