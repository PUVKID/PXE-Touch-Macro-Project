"""동결된16차의 공개 가능한 소스만 전용 Git 복제본으로 복사합니다."""
import hashlib
import json
from pathlib import Path
import shutil

source=Path('D:/2026/PXE Project/16차/source')
repo=source/'work/public-repo'
assert (repo/'.git').is_dir()
destination=repo/'16차/source'
root_names=['.gitignore','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','README.md','빌드.ps1','빌드방법.txt']
files=[source/name for name in root_names]
files += [source/'app/build.gradle']
for directory in ['app/src','gradle','tools']:
    files += [path for path in (source/directory).rglob('*') if path.is_file() and '__pycache__' not in path.parts]
records=[]
for path in sorted(set(files)):
    relative=path.relative_to(source)
    assert not any(part in {'work','evidence','.codex','.git'} for part in relative.parts)
    assert path.suffix.lower() not in {'.apk','.jks','.keystore','.key','.pem','.env','.db','.sqlite'}
    assert path.name not in {'local.properties','key.properties','최종.json','ROUTING-LESSONS.md'}
    assert path.stat().st_size < 8*1024*1024
    target=destination/relative
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(path,target)
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    assert hashlib.sha256(target.read_bytes()).hexdigest()==digest
    records.append({'path':relative.as_posix(),'bytes':path.stat().st_size,'sha256':digest})
(source/'evidence/public16-source-export.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('공개 소스 복사와 바이트 대조:',len(records),'개')
