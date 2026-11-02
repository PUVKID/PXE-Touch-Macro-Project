"""실제 일반 폴더와 전용 장치만 사용합니다."""
from pathlib import Path
PROJECT=Path('D:/2026/PXE Project')
ROOT=PROJECT/'20차/source'
CONTROL=PROJECT/'work/20차_24시간검증'
BUILD=PROJECT/'work/build20/source'
PRODUCT=PROJECT/'19차/source'
SDK=PROJECT/'코드/NEXTools/android-sdk'
ADB=SDK/'platform-tools/adb.exe'
JDK=PROJECT/'work/tools/jdk-17.0.20.1+1'
GRADLE=PROJECT/'work/tools/gradle-8.13/bin/gradle.bat'
PC=PROJECT/'19차/PXE_19차_v1.9_PC용.apk'
PHONE=PROJECT/'19차/PXE_19차_v1.9_노트10플러스.apk'
PC_SHA='46017d7f2bb2736871776d69eb93ce5d28dc8838294999e2e129bee4bf358276'
PHONE_SHA='ac257ce52d430aac0918f78f282ca1180ff20c3114d6513c2c9b13ff88ce03a6'
PREFIX=[str(ADB),'-P','5043','-s','emulator-5598']
