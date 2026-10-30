"""독립 실행 원문·동결·요구 사항 대조를 최종 검토문에 결합합니다."""
import hashlib
import json
import sys
from pathlib import Path
import datetime

ROOT = Path(__file__).resolve().parents[1]
E = ROOT / 'evidence'
sys.path.insert(0, str(Path.home() / '.codex/hooks'))
from routing_stop import source_snapshot

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

state = read(ROOT / '.codex/routing-state/df1ea7273e46d16ba9664afc.json')
snapshot = source_snapshot(ROOT)
assert snapshot == state['verification_dispatch']['source']
assert state['repair_used'] == 13 and state['verification_failures'] == 1
assert state['verification_dispatch']['task_context'] == {k: state[k] for k in state['verification_dispatch']['task_context']}
required = read(E / 'verifier15-final3-required-summary.json')
focused = read(E / 'verifier15-final3-focused-summary.json')
assert len(required['results']) == 9 and all(x['status'] == 'passed' for x in required['results'])
assert len(focused['results']) == 2 and all(x['status'] == 'passed' for x in focused['results'])
assert required['elapsed_seconds'] < 720 and focused['elapsed_seconds'] < 240
assert required['source_before']['digest'] == required['source_after']['digest'] == snapshot['digest']
assert required['source_before']['files'] == required['source_after']['files'] == snapshot['files'] == 120
impact = read(E / 'verifier15-global-impact4.json')
assert impact['reuse_required52'] is True and impact['current_global_digest'] == snapshot['global_digest']
assert required['source_before']['global_digest'] == impact['previous_global_digest']
assert required['source_after']['global_digest'] == impact['current_global_digest']
assert focused['source_before'] == focused['source_after'] == snapshot
records = [read(E / f'verifier15-final3-required-{i}.json') for i in range(8)]
small = read(E / 'verifier15-final3-small-0.json')
new = [read(E / f'verifier15-final3-focused-{i}.json') for i in range(2)]
gradle = read(E / 'verifier15-final3-powershell-gradle-0.json')
assert all(x['passed'] for x in records + [small] + new + [gradle])
assert sum(x['actual_tests'] for x in records[3:]) + small['actual_tests'] == 52
assert sum(x['actual_tests'] for x in new) == 12
assert records[0]['actual_tests'] == 86 and gradle['unit_totals']['tests'] == 86 and gradle['lint_issues'] == 0
artifacts = {}
for path in [ROOT.parent / 'PXE_15차_v1.5_PC용.apk', ROOT.parent / 'PXE_15차_v1.5_노트10플러스.apk', ROOT / 'work/build/app/outputs/apk/androidTest/release/app-release-androidTest.apk']:
    artifacts[str(path)] = dict(bytes=path.stat().st_size, sha256=hashlib.sha256(path.read_bytes()).hexdigest())
requirements = [
    ('엄격 화면 정보의 OCR 미호출과 실제 오류 단계·요청/실제 방식 구분', 'MacroService.find의 ACCESS 조기반환 및 recognitionStage/Detail 확인. RecognitionModeTest5는 ACCESS 노출부재/보안6 제어주입의 단계분리, 0캡처와 기본/즉시 재확인 횟수를 직접 통과. 보안주입은 사용자 블루스택 재현으로 취급하지 않음.'),
    ('안전한 캡처 간격 후퇴와 성공 회복 및 새 시간 정책', '순수/Gradle86 중 Pacer6의500/1100·정확한2배·12초상한·절반회복 확인. 새 CaptureRecoveryTest14배치PASS. 첫 메서드의구4/8/8만 명세API표의 응답→요청2배/4배/2배로 명시개정, 실제4response/4capture/1entry·UI·45초·쿨다운과 다른2메서드 유지 확인.'),
    ('기존 무한/유한 대기·정지·일시정지·대상 앱·단일 OCR 및 이미지 수명', '새동결의 TextConfirmation4와 core20 및 화면경계14 모두 실제PASS. 기존32 재사용없음. 기존 Task 완료해제/대상 앱 확인/최대2실캡처 코드가 유지됨.'),
    ('전체 OCR·영역 중심·동일글자 순번·우선순위 캐시 의미 보존', '14→15 ocrHit/inside/accessibleHit2126문자 동일. ROI제품도입제외. RecognitionRegionTest3 실제PASS; 경계1은같은실제Text의제품필터 full/6px일치·중심제외null·원좌표실제TAP 결합검사이며 두새프레임통합으로 과장하지 않음. 다른2검사는 실제OCR 순번/2순위 단일캡처캐시 통합.'),
    ('기본/고급 설정 단순화·적용 범위·취소/저장·기존 값 보존', 'PxeRecognitionSettingsTest4 실제PASS. 목록/개별/안내의 기본 방식·유한대기 노출, 고급 보정값 편집·다른모드와저장매크로 보존, 즉시진입 false기본/취소/저장/재열기 확인. word_source 안내는 글자탭에서추가하는경로에 한정됨.'),
    ('기존 안내·라이트/다크·나눔고딕Bold·48dp·작은 화면 기능', '기본UI6 및720x1280/density360의320dp UI6 실제PASS, 이후1080x2280/density420 복구. 최신기본/작은화면/신규폼 PNG를 직접열어 글꼴/색·긴문구/접기·탐색·입력·설명·버튼을 검토. 스크롤 아래 항목은 한 화면 전체표시로 주장하지 않음.'),
    ('14→15 실제 업데이트 및 앱 ID·버전·개인 서명·ABI·CRC·원본 보존', 'check_upgrade2 actualPASS. check_artifacts로112이전원본·두APK/PXE/15·1.5.0/기존서명/PC ARM64+x86_64 및폰 ARM64·CRC를직접통과. 키/개인profile/사용자블루스택조작0.'),
    ('독립 단위·린트·UTF8 검사', '순수86, 독립 Gradle --rerun-tasks 단위86/13클래스/실패0/오류0/skip0 및 lint0. 중앙verify는이후별도3gate실행. UTF8엄격디코딩/대체문자검사는 의미상완전무오류 보장이 아님.'),
    ('독립 검증·동결·실패 이력 보존', '실제부모등록의서로다른implementer/verifier ID와현재누적에결합. source120전후동일. 필수52실행중전역변경을차단·재독·독립영향평가·부모재등록후근거연결. 새전역에서52를재실행했다고소급하지않음. 추가12/Gradle/영수증은새전역에서검증. 정식FAIL11 및모든과거실패보존. 검증자제품/설정/검사명세수정0.'),
]
review = dict(decision='PASS', reviewed_kst=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat(),
              agent_id='/root/pxe15_verifier', assigned_role='verifier', assigned_model='gpt-6.1-sol', assigned_effort='high',
              assurance='부모 네이티브 요청·로컬 등록·직접 실행 근거입니다. 서버 보증 모델 신원 인증이 아닙니다.',
              requirements=[dict(requirement=r, passed=True, evidence=e) for r, e in requirements], source=snapshot,
              state={k: state[k] for k in ('session_id','task_id','mode','repair_used','input_fix_used','verification_failures','stop_failures')},
              device_tests=64, required_device_tests=52, focused_device_tests=12, pure_unit_tests=86, gradle_unit_tests=86,
              required_elapsed_seconds=required['elapsed_seconds'], focused_elapsed_seconds=focused['elapsed_seconds'],
              direct_evidence=dict(required='evidence/verifier15-final3-required-summary.json', focused='evidence/verifier15-final3-focused-summary.json', gradle='evidence/verifier15-final3-powershell-gradle-0.json'),
              execution_contexts=dict(required=[x['state'] for x in records], small=small['state'], focused=[x['state'] for x in new], gradle=gradle['state'], final_registration=state['verification_dispatch']['runtime_evidence_path']),
              artifacts=artifacts, global_change_assessment=impact,
              log_collection='실명세argv는각child JSON에기록. shell=False/WindowsJob 종료와개별/총상한유지. 외부4KiB요약·별도원문64KiB상한/엄격UTF8. 각원문 original_bytes/captured_bytes/omitted/SHA 명시. 기존도구의내부16KiB수집과외부집계를구분.',
              limitations=['사용자 실제 블루스택/게임/개인프로필/노트10+ 실기기/다중5개/24시간운용미검증', '필수52는이전전역32566B참조추가중실행되어마지막global가드exit1. 제품소스불변과지침영향없음을독립판단해원실행근거로유지하며새전역실행으로소급하지않음', 'API30의1100ms정책은단위검사·코드검토범위이며실기기는API31소유AVD', '경계1검사는동일실제OCR Text필터와좌표제스처결합범위', '공개GitHub반영·원격커밋은부모담당이며이영수증의직접검증범위밖', '실측비용·서버모델신원확인불가,한글UTF8검사와의미상무오류보장은다름'])
(E / 'verifier15-review-pass13.json').write_text(json.dumps(review, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps(dict(review='evidence/verifier15-review-pass13.json', source=snapshot, device_tests=64, unit_tests=86, gradle_tests=86),ensure_ascii=False))
