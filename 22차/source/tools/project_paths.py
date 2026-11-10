"""공개 사본의 실제 소스·작업 폴더와 사용자 도구 설정을 연결합니다."""
import os
from pathlib import Path

PROJECT = Path(os.environ.get('PXE_PROJECT_ROOT') or str(Path(__file__).resolve().parents[3]))
CANONICAL = PROJECT / '22차/source'
BUILD = Path(os.environ.get('PXE_BUILD_ROOT') or str(PROJECT / 'work/build22/source'))
DELIVERY = PROJECT / '22차'
SDK = Path(os.environ.get('ANDROID_HOME', str(PROJECT / '코드/NEXTools/android-sdk')))
JDK = Path(os.environ.get('JAVA_HOME', str(PROJECT / 'work/tools/jdk-17.0.20.1+1')))
GRADLE = Path(os.environ.get('PXE_GRADLE', str(PROJECT / 'work/tools/gradle-8.13/bin/gradle.bat')))
SIGNING = Path(os.environ.get('NEX_SIGNING_DIR', str(PROJECT / '코드/NEXSigning')))
