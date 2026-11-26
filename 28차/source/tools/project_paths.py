"""28차의 실제 기준 소스·빌드 사본·도구 경로를 한곳에 둡니다."""
from pathlib import Path
import os

_source = Path(__file__).resolve().parents[1]
if _source.parent.name == '28차':
    PROJECT = _source.parent.parent
elif _source.parent.name == 'build28' and _source.parents[1].name == 'work':
    PROJECT = _source.parents[2]
else:
    raise RuntimeError('28차 기준 소스 또는 실제 build28 폴더에서 실행하세요.')
CANONICAL = PROJECT / '28차/source'
BUILD = PROJECT / 'work/build28/source'
DELIVERY = PROJECT / '28차'
def runtime(relative, variable):
    explicit = os.environ.get(variable)
    if explicit:
        return Path(explicit)
    current = PROJECT / relative
    if current.exists():
        return current
    previous = Path('D:/2026/PXE Project') / relative
    if previous.exists():
        return previous
    raise RuntimeError('실제 실행 환경 경로를 지정하세요: ' + variable)
SDK = runtime('코드/NEXTools/android-sdk', 'ANDROID_HOME')
JDK = runtime('work/tools/jdk-17.0.20.1+1', 'JAVA_HOME')
GRADLE = runtime('work/tools/gradle-8.13/bin/gradle.bat', 'PXE_GRADLE')
SIGNING = runtime('코드/NEXSigning', 'NEX_SIGNING_DIR')
