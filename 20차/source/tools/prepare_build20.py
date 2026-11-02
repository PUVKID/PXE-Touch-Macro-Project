"""19차 원문을 보존하면서 별도 일반 빌드 폴더에 시험만 합성합니다."""
from pathlib import Path
import hashlib,json,shutil,stat,sys
from project_paths import *
from soak_contract import atomic_json


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def ordinary(path):
    if path.is_symlink() or path.is_junction() or path.stat().st_file_attributes&stat.FILE_ATTRIBUTE_REPARSE_POINT:
        raise RuntimeError('연결 경로 금지: '+str(path))


def main():
    assert sha(PC)==PC_SHA and sha(PHONE)==PHONE_SHA,'기존 제품 APK 변경'
    BUILD.mkdir(parents=True,exist_ok=True);ordinary(BUILD)
    original=json.loads((PROJECT/'work/build19/source/work/source-sync.json').read_text(encoding='utf-8'))['files']
    baseline=CONTROL/'19차_고정기준.json'
    if baseline.exists():assert json.loads(baseline.read_text(encoding='utf-8'))==original,'기존 기준 덮어쓰기 금지'
    else:atomic_json(baseline,original)
    for row in original:
        source=PRODUCT/row['path'];ordinary(source)
        assert source.stat().st_size==row['bytes'] and sha(source)==row['sha256'],row['path']
        target=BUILD/row['path'];target.parent.mkdir(parents=True,exist_ok=True)
        if target.exists():ordinary(target)
        shutil.copyfile(source,target);assert target.read_bytes()==source.read_bytes(),row['path']
    source=ROOT/'androidTest/LongRunOcrTest.java'
    target=BUILD/'app/src/androidTest/java/kr/yunseok/touchmacro/LongRunOcrTest.java'
    shutil.copyfile(source,target);assert sha(source)==sha(target)
    (BUILD/'local.properties').write_bytes(('sdk.dir='+SDK.as_posix()+'\n').encode('ascii','backslashreplace'))
    destination=BUILD/'work/target19.apk';destination.parent.mkdir(exist_ok=True);shutil.copyfile(PC,destination);assert sha(destination)==PC_SHA
    atomic_json(CONTROL/'빌드사본_대조.json',{'source_files':len(original),'main_unchanged':True,'test_overlay':str(target),'overlay_sha256':sha(target),'new_links':0,
               'pc_sha256':sha(PC),'phone_sha256':sha(PHONE),'baseline_sha256':sha(baseline)})
    print('19차 선택 원문 '+str(len(original))+'개와 실제 일반 빌드 사본 대조 PASS / 제품 원본 변경 0')


if __name__=='__main__':main()
