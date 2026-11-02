"""이번 차수 텍스트의 엄격 UTF-8과 대체 문자를 검사합니다."""
from pathlib import Path
from project_paths import ROOT
count=0
for path in ROOT.rglob('*'):
    if not path.is_file() or any(part in ('.codex','evidence','work','__pycache__') for part in path.relative_to(ROOT).parts):continue
    if path.suffix not in ('.py','.java','.md','.json','.ps1','.txt'):continue
    value=path.read_bytes().decode('utf-8-sig')
    assert '\ufffd' not in value,str(path)
    count+=1
print('엄격 UTF-8 검사 PASS: '+str(count)+'개')
