"""필수 검사 뒤 정상 화면을 보존하고 소유 장치의 작은 화면을 추가 검사합니다."""
import json
import re
import time
from pathlib import Path
from verifier_final6 import ADB, EVIDENCE, PYTHON, ROOT, now, run, save, snapshot

def main():
    summary = json.loads((EVIDENCE/'verifier-final6-summary.json').read_text(encoding='utf-8'))
    assert summary['total_tests']==46 and summary['unchanged']
    prefix = [ADB,'-P','5043','-s','emulator-5598']
    out = ROOT/'work/verifier-final6-captures'
    out.mkdir(exist_ok=True)
    run('verifier-final6-pull-normal',prefix+['pull','/sdcard/Android/data/kr.yunseok.touchmacro/files','D:/PXE14Build/source/work/verifier-final6-captures'],30)
    _, size_text = run('verifier-final6-wm-size-before',prefix+['shell','wm','size'],10)
    _, density_text = run('verifier-final6-wm-density-before',prefix+['shell','wm','density'],10)
    assert size_text.strip()=='Physical size: 1080x2280', size_text
    assert density_text.strip()=='Physical density: 420', density_text
    started = summary['device_batches'][0]['started_kst']
    import datetime
    elapsed = (datetime.datetime.fromisoformat(now())-datetime.datetime.fromisoformat(started)).total_seconds()
    assert elapsed<570, '작은 화면 검사를 시작할 공유 예산 부족'
    before = snapshot()
    try:
        run('verifier-final6-wm-small',prefix+['shell','wm','size','840x1493'],10)
        run('verifier-final6-small-ui6',[PYTHON,'-B','-X','utf8','tools/check_device.py','PxeDesignReviewTest','PxeGuideFlowTest','PxeRedesignTest'],min(120,720-elapsed-20),expected=6)
        small = ROOT/'work/verifier-final6-small-captures'
        small.mkdir(exist_ok=True)
        run('verifier-final6-pull-small',prefix+['pull','/sdcard/Android/data/kr.yunseok.touchmacro/files','D:/PXE14Build/source/work/verifier-final6-small-captures'],30)
    finally:
        run('verifier-final6-wm-restore',prefix+['shell','wm','size','reset'],10)
        _, restored_size = run('verifier-final6-wm-size-after',prefix+['shell','wm','size'],10)
        _, restored_density = run('verifier-final6-wm-density-after',prefix+['shell','wm','density'],10)
        assert restored_size==size_text and restored_density==density_text, '시험 해상도·밀도 원복 불일치'
    after = snapshot()
    elapsed = (datetime.datetime.fromisoformat(now())-datetime.datetime.fromisoformat(started)).total_seconds()
    save('verifier-final6-small-summary.json', {'completed_kst':now(),'normal_size':size_text.strip(),'normal_density':density_text.strip(),'temporary_size':'840x1493','temporary_width_dp':320,'restored_size':restored_size.strip(),'restored_density':restored_density.strip(),'required_tests':46,'additional_tests':6,'shared_elapsed_seconds':elapsed,'shared_budget_seconds':720,'source_and_artifacts_unchanged':before==after,'snapshot':after})
    assert before==after
    assert elapsed<720

if __name__=='__main__':
    main()
