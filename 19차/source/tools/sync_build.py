"""연결을 만들지 않고 선택 소스를 실제 빌드 폴더로 복사·대조합니다."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import shutil
import stat
from project_paths import BUILD, CANONICAL, SDK

SKIP = {'.git', '.gradle', '.codex', 'evidence', 'work', '__pycache__', 'build'}

def ordinary(path):
    if path.is_symlink() or path.is_junction() or path.stat().st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT:
        raise RuntimeError('빌드 경로에 폴더 연결을 사용할 수 없습니다: ' + str(path))

def selected(root):
    for folder, directories, names in os.walk(root, followlinks=False):
        directories[:] = sorted(name for name in directories if name not in SKIP)
        for name in directories:
            ordinary(Path(folder) / name)
        for name in sorted(names):
            if name == 'local.properties':
                continue
            path = Path(folder) / name
            ordinary(path)
            yield path

def main():
    ordinary(CANONICAL)
    ordinary(BUILD)
    manifest = []
    for source in selected(CANONICAL):
        relative = source.relative_to(CANONICAL)
        target = BUILD / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            ordinary(target)
        shutil.copyfile(source, target)
        before, after = source.read_bytes(), target.read_bytes()
        if before != after:
            raise RuntimeError('기준 소스와 빌드 사본이 다릅니다: ' + relative.as_posix())
        manifest.append({'path': relative.as_posix(), 'bytes': len(before),
                         'sha256': hashlib.sha256(before).hexdigest()})
    actual = {path.relative_to(BUILD).as_posix() for path in selected(BUILD)}
    if actual != {entry['path'] for entry in manifest}:
        raise RuntimeError('실제 빌드 사본에 선택 소스 외 파일이 있습니다')
    # Java properties는 한글 경로를 ASCII 유니코드 이스케이프로 기록합니다.
    (BUILD / 'local.properties').write_bytes(('sdk.dir=' + SDK.as_posix() + '\n').encode('ascii', 'backslashreplace'))
    output = BUILD / 'work/source-sync.json'
    output.parent.mkdir(exist_ok=True)
    output.write_bytes((json.dumps({'at_kst': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat(),
        'canonical': str(CANONICAL), 'build': str(BUILD), 'new_links': 0, 'files': manifest}, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
    print('실제 빌드 사본 원문·해시 대조: ' + str(len(manifest)) + '개 통과 / 새 연결 0')

if __name__ == '__main__':
    main()
