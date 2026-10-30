from pathlib import Path
import shutil,json,hashlib,datetime
base=Path('D:/2026/PXE Project'); prev=base/'13차'; delivery=base/'14차'; old=prev/'source'; new=delivery/'source'
skip={'work','evidence','.codex','.git','__pycache__'}
if new.exists(): raise SystemExit('새 소스 폴더가 이미 있어 복사를 중단합니다')
shutil.copytree(old,new,ignore=shutil.ignore_patterns(*skip))
entries=[]
for p in old.rglob('*'):
    if not p.is_file() or any(x in skip for x in p.relative_to(old).parts): continue
    entries.append({'path':str(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest().upper()})
for p in prev.glob('*.apk'):
    entries.append({'path':str(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest().upper()})
(delivery/'13차_보존기준.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2),encoding='utf-8')
(delivery/'evidence').mkdir(exist_ok=True); (new/'evidence').mkdir(); (new/'docs').mkdir()
shutil.copyfile(Path('C:/Users/PUVKID/.codex/generated_images/01a131ad-d167-7873-9aa6-4d81ff131c60/exec-b108a0f5-adbb-47b7-ac0a-1077c5a82025.png'),delivery/'evidence/탐지홈_라이트다크_시안.png')
spec=json.loads((old/'.codex/routing-checks.json').read_text(encoding='utf-8-sig'))
(delivery/'검사명세_중앙.json').write_text(json.dumps(spec,ensure_ascii=False,indent=2),encoding='utf-8')
record={'한국시각':datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat(),'작업':'새 디자인 요청, 이전 OCR 수정 완료 작업과 별개','전역지침':{'path':'C:/Users/PUVKID/.codex/AGENTS.md','bytes':Path('C:/Users/PUVKID/.codex/AGENTS.md').stat().st_size,'sha256':hashlib.sha256(Path('C:/Users/PUVKID/.codex/AGENTS.md').read_bytes()).hexdigest()},'reference_thread':'01a15d1a-cd8a-7dc1-8721-cb9dff52a458','reference_title':'🔄 모델 변경 EXE Project','최신정정':'디자인은 다르게, 설치 플러그인과 제작 방법은 활용','baseline_files':len(entries),'기존프로젝트경계':str(old/'.codex'),'새root':str(new),'기존docs':'없음','분류':'시각 재디자인/일반 구현. 인식엔진 변경 금지. build Sol/medium 및 독립 verifier Sol/high','비고':'ImageGen 시안은 부모가 자율 진행 요청에 따라 구현 기준으로 제안/선정하며 사용자 직접 승인으로 표현하지 않음'}
(delivery/'evidence/시작점검.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
print('새 소스 복사 및 13차 보존 기준',len(entries),'개 기록')
print('D 여유 GiB',round(shutil.disk_usage(base).free/1024**3,2))