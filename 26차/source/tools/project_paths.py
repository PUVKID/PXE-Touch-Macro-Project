"""26차의 실제 기준 소스·빌드 사본·도구 경로를 한곳에 둡니다."""
from pathlib import Path

_source = Path(__file__).resolve().parents[1]
if _source.parent.name == '26차':
    PROJECT = _source.parent.parent
elif _source.parent.name == 'build26' and _source.parents[1].name == 'work':
    PROJECT = _source.parents[2]
else:
    raise RuntimeError('26차 기준 소스 또는 실제 build26 폴더에서 실행하세요.')
CANONICAL = PROJECT / '26차/source'
BUILD = PROJECT / 'work/build26/source'
DELIVERY = PROJECT / '26차'
SDK = PROJECT / '코드/NEXTools/android-sdk'
JDK = PROJECT / 'work/tools/jdk-17.0.20.1+1'
GRADLE = PROJECT / 'work/tools/gradle-8.13/bin/gradle.bat'
SIGNING = PROJECT / '코드/NEXSigning'
