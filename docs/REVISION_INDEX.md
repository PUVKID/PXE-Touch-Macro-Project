<a name="️-차수별-독립-프로젝트-안내"></a>
# 차수별 독립 프로젝트 안내

[프로젝트 첫 화면](../README.md)에서 전체 구현을 읽고 아래에서 실제 차수·최종본·부분 자료를 선택할 수 있습니다. 각 상세 페이지에는 해당 코드·설정·자료 종류에 맞는 기능·사용 조건·실제 관측과 전체 비교를 연결했습니다.

| 차수 | 핵심 변경 | 자료 |
| --- | --- | --- |
| 기본 | NEXSource2의 편집·서비스·제어 소스와 NEXSource3의 일부 wrapper 는 완전성 범위가 다릅니다. | [자료 폴더](../코드) · [같은 수준 상세](versions/기본.md#user-content-reading-level-5) · [전체 비교](revision-audit/기본.md) |
| 1차 | MainActivity의 편집, Store의 JSON 저장, Step의 검증, MacroService의 제스처가 이어집니다. | [자료 폴더](../1차) · [같은 수준 상세](versions/1차.md#user-content-reading-level-5) · [전체 비교](revision-audit/1차.md) |
| 2차 | 글자 동작·OCR 미리 보기 JSON과 사용법·당시 검증 기록을 대조합니다. | [자료 폴더](../2차) · [같은 수준 상세](versions/2차.md#user-content-reading-level-5) · [전체 비교](revision-audit/2차.md) |
| 3차 | FloatingController와 GesturePaths가 별도 제어 창·녹화·곡선·다중 터치 구성으로 연결됩니다. | [자료 폴더](../3차) · [같은 수준 상세](versions/3차.md#user-content-reading-level-5) · [전체 비교](revision-audit/3차.md) |
| 4차 | 새로 고침·글자 탐지·선택 매크로·복귀가 Watch 흐름이며 영역과 알림 아이콘도 바뀝니다. | [자료 폴더](../4차) · [같은 수준 상세](versions/4차.md#user-content-reading-level-5) · [전체 비교](revision-audit/4차.md) |
| 5차 | 탐지 기본 15회는 저장 매크로 전체 반복과 곱하지 않고 WatchConfig와 원본을 분리합니다. | [자료 폴더](../5차) · [같은 수준 상세](versions/5차.md#user-content-reading-level-5) · [전체 비교](revision-audit/5차.md) |
| 6차 | 최대 6개 비중첩 우선 영역, 대상 앱, 중복 잠금과 완료 후 대기를 구성하고 임시 녹화로 준비합니다. | [자료 폴더](../6차) · [같은 수준 상세](versions/6차.md#user-content-reading-level-5) · [전체 비교](revision-audit/6차.md) |
| 7차 | 녹스 5개 동 시 실행을 준비했지만 당시 프로그램 미발견·공간 조건으로 실행을 시작하지 않았습니다. | [자료 폴더](../7차) · [같은 수준 상세](revision-audit/7차.md#user-content-reading-level-5) · [전체 비교](revision-audit/7차.md) |
| 8차 | QueryLane의 화면 읽기와 ReaderResponsivenessTest의 응답 경로를 연결해 봅니다. | [자료 폴더](../8차) · [같은 수준 상세](versions/8차.md#user-content-reading-level-5) · [전체 비교](revision-audit/8차.md) |
| 9차 | 설정 없는 플로팅 재생은 시작하지 않고 CapturePacer·CaptureRecoveryTest가 늦은 캡처와 오류를 다룹니다. | [자료 폴더](../9차) · [같은 수준 상세](versions/9차.md#user-content-reading-level-5) · [전체 비교](revision-audit/9차.md) |
| 10차 | MainActivity의 하단 탐색·카드 UI와 PXE 명칭의 변경을 원래 동작과 구분합니다. | [자료 폴더](../10차) · [같은 수준 상세](versions/10차.md#user-content-reading-level-5) · [전체 비교](revision-audit/10차.md) |
| 11차 | AUTO에서 접근성 읽기가 늦으면 OCR로 전환하는 경로와 순수 회귀·UTF-8·기기 도구를 함께 다룹니다. | [자료 폴더](../11차) · [같은 수준 상세](versions/11차.md#user-content-reading-level-5) · [전체 비교](revision-audit/11차.md) |
| 12차 | 글자 터치·대기의 계속 기다리기와 이전 실행·일시 정지 뒤 도착한 응답의 수명을 구분합니다. | [자료 폴더](../12차) · [같은 수준 상세](versions/12차.md#user-content-reading-level-5) · [전체 비교](revision-audit/12차.md) |
| 13차 | 접힌 패널 정지·연결 종료, 미완료 캡처 최대 2개와 실제 OCR Task 하나를 관리합니다. | [자료 폴더](../13차) · [같은 수준 상세](versions/13차.md#user-content-reading-level-5) · [전체 비교](revision-audit/13차.md) |
| 14차 | 청 록 UI·48dp·모션 설정과 TEXT_TAP 글자 소실 시 원래 waitForever 대기로 돌아가는 흐름이 바뀝니다. | [자료 폴더](../14차) · [같은 수준 상세](versions/14차.md#user-content-reading-level-5) · [전체 비교](revision-audit/14차.md) |
| 15차 | 계속 기다리기·인식 후 추가 대기 생략·목록 즉시 진입은 적용 범위가 다릅니다.<br>즉시 진입은 기본 꺼짐입니다. | [자료 폴더](../15차) · [같은 수준 상세](versions/15차.md#user-content-reading-level-5) · [전체 비교](revision-audit/15차.md) |
| 16차 | 기본 꺼짐 adaptiveWait는 같은 인식 방식·영역의 인접 조건에서 다음 글자 사전 부재가 확인될 때만 적용됩니다. | [자료 폴더](../16차) · [같은 수준 상세](versions/16차.md#user-content-reading-level-5) · [전체 비교](revision-audit/16차.md) |
| 17차 | 조회 하나·새 화면 재확인·완료 OCR 재시도와 0.25~2초 간격을 유지하고 실제 연결 해제를 구분합니다. | [자료 폴더](../17차) · [같은 수준 상세](versions/17차.md#user-content-reading-level-5) · [전체 비교](revision-audit/17차.md) |
| 18차 | 실제로 완료한 뒤 수를 올려7/15 완료와 8회째 실행 중을 나누고 단계·색 이외의 상태 글자를 표시합니다. | [자료 폴더](../18차) · [같은 수준 상세](versions/18차.md#user-content-reading-level-5) · [전체 비교](revision-audit/18차.md) |
| 19차 | 오래된 좌표·불필요한 뒤로가기·중복 해제를 막고 대상 앱을 새로 확인하며 앞선 완료 횟수를 유지합니다. | [자료 폴더](../19차) · [같은 수준 상세](versions/19차.md#user-content-reading-level-5) · [전체 비교](revision-audit/19차.md) |
| 실행 점검-2차 | 보존된 캡처 도구의 PNG CRC·폭·높이·SHA와 명령 기록을 확인합니다. | [자료 폴더](../실행점검/2차) · [같은 수준 상세](versions/실행점검-2차.md#user-content-reading-level-5) · [전체 비교](revision-audit/실행점검-2차.md) |
| 최종.json | 글자·좌표·재시도·대기·스크롤 조건을 가진 매크로 자료를 읽습니다. | [자료 폴더](materials/022-최종설정) · [원본 파일](../최종.json) · [같은 수준 상세](materials/022-최종설정/README.md#user-content-reading-level-5) · [전체 비교](versions/최종설정.md) |
| 고유 작업 자료 | ZIP 경로·링크·암호화·크기 검토와 동일 바이트 또는 BOM·개행 대응을 구분하고 고유 소스를 보존합니다. | [자료 폴더](../보존자료/작업소스) · [같은 수준 상세](SOURCE_MATERIALS.md#user-content-reading-level-5) · [전체 비교](REVISION_AUDIT.md) |
| 20차·시험 도구 | OCR·15회 반복·부재와 재등장·중복 차단을 관측하며 호스트와 장치 시간·세대를 대조합니다. | [자료 폴더](../20차) · [같은 수준 상세](versions/20차.md#user-content-reading-level-5) · [전체 비교](revision-audit/20차.md) |
| 21차 | 확인 글자 부재·되돌아갈 글자 우선·새 글자로 처음부터 시작·이전 세대 늦은 응답 차 단을 연결합니다. | [자료 폴더](../21차) · [같은 수준 상세](versions/21차.md#user-content-reading-level-5) · [전체 비교](revision-audit/21차.md) |
| 22차 | 작은 진행 창 이동·접기와 대기 흐름을 보강하고 공개 소스와 운영 자료를 분리합니다. | [자료 폴더](../22차) · [같은 수준 상세](versions/22차.md#user-content-reading-level-5) · [전체 비교](revision-audit/22차.md) |
| 23차 | OCR584 →32 →K F64 MLP·시범 2·학습 192·자동 터치 1·종료 뒤 입력 0·가져온 뒤 64를 버전에 결합합니다. | [자료 폴더](../23차) · [같은 수준 상세](versions/23차.md#user-content-reading-level-5) · [전체 비교](revision-audit/23차.md) |
| 24차 | 1160차원 OCR +RGB 정책·구형 584호환·동일 future 대기·제어창수리와 정상 빌드 13의 단위 44 클래스 280개 통과를 구분합니다. | [자료 폴더](../24차) · [같은 수준 상세](versions/24차.md#user-content-reading-level-5) · [전체 비교](revision-audit/24차.md) |
| 28차 | 2.8.2 · 원문 완전 일치·느린 자동 스크롤·가로 글자 단축키·하단 직접 편집·26차 기반 내부 정리 | [자료](../28차) · [같은 수준 상세](versions/28차.md#user-content-reading-level-5) · [전체 비교](revision-audit/28차.md) |
| 27차 · 준비본 | 캡처 대기와 상태 표시·색상을 정리했습니다. 60분 종료 근거가 없어 장기 완료 판정은 보류합니다. | [자료 폴더](../27차) · [같은 수준 상세](versions/27차.md#user-content-reading-level-5) · [전체 비교](revision-audit/27차.md) |
| 26차 | 16차의 기본 매크로·UI를 기준으로 별도 AI를 제외하고 글자 비교·창 응답·축소를 보강합니다. | [자료 폴더](../26차) · [같은 수준 상세](versions/26차.md#user-content-reading-level-5) · [전체 비교](revision-audit/26차.md) |
| 최종 | 26차 공용 APK·전체 소스 패키지 ZIP·사용 안내입니다. | [자료 폴더](../최종) · [같은 수준 상세](../최종/README.md#user-content-reading-level-5) · [전체 비교](revision-audit/최종.md) |

23차는 [공용 APK 단일 납품](../23차/공용APK_안내.md)와 [현재 검증 범위](../23차/검증_결과.md)를 함께 확인합니다. 기존 차수·비교 링크는 보존합니다.


## 24차 최종 납품과 실제 확인·2026-11-20

현재 최종 납품은 24차 공용 APK·APK1개를 담은 ZIP·사용 안내 세 파일입니다. 실제 검사와 정상 빌드의 범위는 아래 확정 표를 따릅니다.

| 확인 항목 | 실제 결과 | 해석 범위 |
| --- | --- | --- |
| 기능 검사 | 1개 통과, 한 Watch 사이클에서 동작 15회·완료 1회 | 매크로 전체 15 사이클의 측정이 아닙니다. |
| 기능 시간 | 사이클 4035ms, 측정 전체 4629ms | 합성 단일 환경의 관측이며 실제 게임 속도가 아닙니다. |
| 시각 검사 | 2개 모두 통과 | 첫 검사 시범 2회·학습 256 단계·이동 아이콘 1회, 완료 뒤 추가 입력 0회입니다. |
| 선택 모드 | 대기 상태 실제 대상 클릭 0회·반대편 그림 1회 | 사용 중 거절된 모드 보존과 허용된 스크롤 뒤 모드 해제를 확인했습니다. |
| 복원 | 원래 앱 3개·데이터·메타데이터·설정 복원 확인 | 이전 프로세스 ID 재생성이나 전체 AVD 과거 시점 복원을 뜻하지 않습니다. |
| 정상 빌드 13 | 단위 44 클래스 280개 실제 실행, 실패·오류·건너뜀 0 | 린트 오류 0·경고 4, 최종 소스 291 파일을 대조했습니다. |
| 제품 동일성 | 빌드 12와 manifest·모든 DEX·전체 ZIP payload 동일 | APK 전체 바이트는 서명 블록 4093바이트 차이로 다릅니다. |
| 현재 납품 | APK78473602 바이트·ZIP31271319 바이트·안내 5818바이트 | ZIP에는 같은 APK1개만 있고 CRC209218163을 확인했습니다. |

기존 매크로와 디자인을 유지하며 AI 는 기본 꺼짐입니다. 작은 오프라인 행동 정책으로, 범용 LLM이 아닙니다. 다른 휴대 전화의 실제 가져오기·모든 게임·24시간·1년 무오류 운용과 경량화 완료는 확인하지 않았습니다. 앞선 a7~a16 및 설치·복원 준비의 실패는 보존합니다.


### 이전 표 문구의 출처 구분

이전 표의 `정상빌드6661/280PASS`는 출처가 미확정인 압축 문구로 인용 보존합니다. 현재 빌드 번호나 검사 분모로 사용하지 않습니다. 현재 확정값은 **정상 빌드 13·44 클래스 280개 단위 검사 통과**이며 실패·오류·건너뜀은 0입니다.
