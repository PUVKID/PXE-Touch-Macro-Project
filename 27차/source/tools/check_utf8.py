"""생성물과 라우팅 상태를 제외한 프로젝트 텍스트를 엄격히 디코딩합니다."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
suffixes = {".java", ".gradle", ".xml", ".py", ".md", ".ps1", ".properties",
            ".json", ".txt", ".bat", ".pro", ".obj"}
count = 0
for path in sorted(root.rglob("*")):
    relative = path.relative_to(root)
    if not path.is_file() or relative.parts[0] in {"work", ".codex"}:
        continue
    if path.suffix.lower() not in suffixes and path.name not in {"gradlew", ".gitignore"}:
        continue
    text = path.read_bytes().decode("utf-8", errors="strict")
    if chr(0xfffd) in text:
        raise SystemExit(f"한글 대체 문자 발견: {relative}")
    count += 1
print(f"UTF-8 엄격 디코딩 및 대체 문자 검사: {count}개 통과")
