"""19차의 실제 기준 소스·빌드 사본·도구 경로를 한곳에 둡니다."""
from pathlib import Path

PROJECT = Path('D:/2026/PXE Project')
CANONICAL = PROJECT / '19차/source'
BUILD = PROJECT / 'work/build19/source'
DELIVERY = PROJECT / '19차'
SDK = PROJECT / '코드/NEXTools/android-sdk'
JDK = PROJECT / 'work/tools/jdk-17.0.20.1+1'
GRADLE = PROJECT / 'work/tools/gradle-8.13/bin/gradle.bat'
SIGNING = PROJECT / '코드/NEXSigning'
