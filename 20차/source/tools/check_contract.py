"""실제 제품·별도 시험 빌드·고정 기준을 대조합니다."""
import hashlib,json
from project_paths import *


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


assert sha(PC)==PC_SHA and sha(PHONE)==PHONE_SHA,'19차 납품 APK 변경'
baseline=json.loads((CONTROL/'19차_고정기준.json').read_text(encoding='utf-8'))
assert baseline
for row in baseline:
    source=PRODUCT/row['path'];target=BUILD/row['path']
    assert source.stat().st_size==row['bytes'] and sha(source)==row['sha256'],row['path']
    assert target.read_bytes()==source.read_bytes(),'실제 사본 차이 '+row['path']
overlay=BUILD/'app/src/androidTest/java/kr/yunseok/touchmacro/LongRunOcrTest.java'
assert overlay.read_bytes()==(ROOT/'androidTest/LongRunOcrTest.java').read_bytes()
assert sha(BUILD/'work/target19.apk')==PC_SHA
print('계약 PASS: 19차 원문 '+str(len(baseline))+'개·두 납품 APK 불변·실제 시험 사본 일치')
