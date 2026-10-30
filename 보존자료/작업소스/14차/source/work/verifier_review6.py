"""독립 검사 결과와 작은 화면 검사 실패를 최종 검토문에 결합합니다."""
import datetime
import hashlib
import json
from pathlib import Path
import re
import struct
from verifier_final6 import EVIDENCE, ROOT, SESSION, TASK, now, save, snapshot

def main():
    summary = json.loads((EVIDENCE/'verifier-final6-summary.json').read_text(encoding='utf-8'))
    small = json.loads((EVIDENCE/'verifier-final6-small-ui6.json').read_text(encoding='utf-8'))
    raw = (EVIDENCE/'verifier-final6-small-ui6.txt').read_text(encoding='utf-8')
    match = re.search(r'Tests run: (\d+),\s+Failures: (\d+)',raw)
    assert match and tuple(map(int,match.groups()))==(6,1)
    manifests = []
    for folder in ['verifier-final6-captures','verifier-final6-small-captures']:
        for p in sorted((ROOT/'work'/folder/'files').glob('*.png')):
            data = p.read_bytes()
            manifests.append({'path':p.relative_to(ROOT).as_posix(),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'pixels':list(struct.unpack('>II',data[16:24]))})
    save('verifier-final6-capture-manifest.json',{'recorded_kst':now(),'images':manifests,'note':'각 PNG 크기로 정상1080×2280/추가840×1493 자료를 구분합니다. 작은 화면에서 생성되지 않은 파일은 이전 정상 크기일 수 있습니다.'})
    current = snapshot()
    assert current==summary['before']==summary['after']
    requirements = [
        ('새 OCR 제보에 따른 최소 엔진 수리와 오류 구분',True,'13→14 MacroService diff는 한국어 주석/무한 TEXT_TAP 재확인 null의 100ms 중단 가능한 재탐지/TEXT_NOT_FOUND 설정 진단만 추가했습니다. 기존 ready/sleep·지역 touchTarget·대상 재검사·유한 실패·즉시 진행·OCR/캡처 오류 경로를 보존했습니다. TextConfirmationTest 실제 OCR 화면 변경 방식을 읽고 구현자 RED2 원문과 별도로 최종4개를 직접 통과했습니다. 사용자 과거 원인 확정은 아닙니다.'),
        ('정상 크기 필수46개 독립 실행',True,'verifier-final6-summary.json: upgrade2→UI6→TextConfirmation4→core20→boundary14, 총46/실패0,430.500초<720, 각180/120/90/260/180초 및 내부240초 유지. UI 이후 추가prefs 초기화0. 구현자 이전42/46개와 합산하지 않았습니다.'),
        ('Gradle 단위 및 lint',True,'verifier-final6-gradle.json/txt: 지정 Gradle8.13 --rerun-tasks로27tasks 실제 실행, 종료0/10.250초. XML12클래스80개/실패0/오류0/skip0, lint XML issue0. 중앙 순수80개는 같은 모델 suite의 별도 실행이므로160개 고유 검사로 합산하지 않습니다.'),
        ('기존 차수·앱ID·버전·서명·ABI·압축 무결성',True,'verifier-final6-artifacts.json/txt 직접 실행 종료0/1.047초, 13차 보존98항목 및 양 APK kr.yunseok.touchmacro/PXE/14·1.4.0/기존 SHA256 인증서/phone arm64와PC arm64+x86_64/CRC 통과. 실제13→14 업데이트2도 독립 통과했습니다.'),
        ('시안 체계와 정상 화면의 충실도',True,'두 시안과 이번 독립 UI6에서 생성한1080×2280 라이트·다크 홈, 설정, 동작 캡처를 같은 비교 입력에서 view_image로 직접 확인했습니다. 큰 검색어/24dp 간격/순백·먹색·청록/열린 행/활성 밑줄/실제 상태·설정값/나눔Bold 체계를 확인했습니다. 원벡터·작은 앱아이콘 제어창·평면색·다크 버튼 먹색·스크롤 아래 보조 동작은 명세상 허용 차이입니다. 사전 수동 설정_전체.png 전환 화면 제외는 유지합니다. 최종 캡처와 SHA는 verifier-final6-capture-manifest.json에 있습니다.'),
        ('작은320dp 화면에서 안내 포함UI6 전체 검증',False,'추가840×1493/density420(폭320dp)에서6실행/5PASS/1FAIL, 종료1/26.109초. 유일 실패는 PxeGuideFlowTest→GuidedFlowTest.java:40 drag(850,1700,850,650)→:22 injectInputEvent false입니다. 폭840/높이1493 밖의 고정좌표이며 :31의950/:41의850·1700도1080화면 전제입니다. 제품결함으로 확정하지 않지만 전체 작은 화면 안내 완료를 입증하지 못해 완료를 차단합니다. 신규PxeRedesign4와 시각PxeDesignReview1은 통과했습니다. 검사 소스·assert·기준은 수정하지 않았고 실패를 미지원으로 없애지 않았습니다.'),
        ('실제 정지·대상 앱·수명 및 데이터 경계',True,'핵심20/경계14의 실제 OCR·늦은 결과·중단/일시정지/대상 앱 전환·접근성 해제·오버레이 토큰 검사를 직접 통과했습니다. 새4의 실제 ■ 정지와 BACK_RETRY 미실행도 통과했습니다. 사용자 기기 조작0, 소유 PXE14 이름 확인 후 사용했습니다. 기록에는 새 검색 원문/원본 화면을 수집하지 않는 설정 진단만 추가됐고 실제 검사가 이를 확인합니다.'),
        ('독립 역할·동결·전후 소스 결합',True,'부모 register/runtime evidence의 구현자 /root/pxe14_ocr_wait 및 추가 /root/pxe14_build와 다른 /root/pxe14_verifier입니다. 파일 설정/네이티브 요청 근거이며 서버 신원 보증은 아닙니다. 직접 검사와320실패 후에도source112파일/global/master/양APK/testAPK의 해시는 동일합니다. 검증자의 제품/검사/기준 수정0, evidence/work 기록만 생성했습니다.'),
        ('실패 보존·장치 원복',True,'verifier-final6-failure-first.json과small-ui6.json/txt에02:39:52KST/argv/cwd/exit1/원문17442B SHA/repair6·input12를 먼저 기록했습니다. 성공표식 기반actual_tests=0은실행0건이 아니며 원문 Tests run6/Failures1로 정정 해석했습니다. finally로1080×2280/density420 정확 복구를 전후문자열과종료0으로 확인했습니다. 앞선 수리1~5/RED2/입력보정 이력을 지우지 않았습니다.')
    ]
    evidence_files = list(EVIDENCE.glob('verifier-final6-*.json'))+list(EVIDENCE.glob('verifier-final6-*.txt'))
    review = {'decision':'FAIL','recorded_kst':now(),'session':SESSION,'task':TASK,'mode':'reassess',
              'reviewer':{'agent_id':'/root/pxe14_verifier','role':'verifier','model_requested':'gpt-6.1-sol','effort_requested':'high','server_identity_verified':False},
              'repair_used':6,'input_fix_used':12,'verification_failures_before_verify':0,
              'requirements':[{'requirement':name,'passed':passed,'evidence':evidence} for name,passed,evidence in requirements],
              'required_device_summary':summary,'additional_small_device_result':{**small,'actual_executed_tests_from_failure_summary':6,'failures_from_raw':1,'passes_from_raw':5,'success_marker_count_is_not_executed_count':True},
              'current_snapshot':current,'capture_manifest':'evidence/verifier-final6-capture-manifest.json',
              'evidence_files':[{'path':p.relative_to(ROOT).as_posix(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(evidence_files)],
              'visual_limitations':['시안보드는1219×1290/1008×1561의복수화면이며실제는1080×2280,도구표시970×2048입니다. 픽셀1:1중첩동일성주장은아닙니다.','사용자BlueStacks5개/노트10+실기기/24시간/확대글꼴/API35실기insets미검증.','기존유한설정은자동무한대기로변경하지않으며사용자과거설정·로그미확보로이번재현을과거원인의확정으로표현하지않습니다.'],
              'cost':'API 환산 비용/서버 실행 모델 신원은 확인 불가',
              'assurance':'로컬 운영 검증입니다. 중앙 자동 검사는3gate75초까지이며 긴 기기 검사·Gradle·화면 검토는 독립 직접 실행 근거입니다. 작은 화면 실패 때문에 완료를 차단합니다.'}
    save('verifier-final6-review.json',review)
    print('FAIL 검토문 저장: evidence/verifier-final6-review.json')

if __name__=='__main__':
    main()
