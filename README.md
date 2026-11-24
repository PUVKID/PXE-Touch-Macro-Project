<a name="-pxe-project"></a>
# PXE Project

화면의 글자와 그림을 확인해 터치·스크롤·뒤로가기를 실행하는 Android 매크로 앱입니다.

**현재 앱: 24차·2.4.0** · Android 11 이상 · ARM64 / x86_64 · 오프라인 사용

[사용 안내](최종/사용안내.md) · [24차 상세](docs/versions/24차.md#reading-level-1) · [소스와 빌드](24차/source/README.md) · [확인한 결과](24차/검증_결과.md)

<!-- project-levels:start -->
## 읽는 수준 선택

| 수준 | 읽을 내용 |
| --- | --- |
| [LEVEL 1 - Beginner](docs/읽기안내/레벨1.md) | 쉬운 설명과 실제 사용 예 |
| [LEVEL 2 - Explorer](docs/읽기안내/레벨2.md) | 기능과 처리 흐름 |
| [LEVEL 3 - Practitioner](docs/읽기안내/레벨3.md) | 설정과 사용 순서 |
| [LEVEL 4 - Advanced](docs/읽기안내/레벨4.md) | 구현 구조와 오류 처리 |
| [LEVEL 5 - Expert](docs/읽기안내/레벨5.md) | 코드·측정 근거와 전체 비교 |
<!-- project-levels:end -->

<a id="current-24-final"></a>
## 24차·화면 인식과 시범 학습

| 기능 | 동작 |
| --- | --- |
| 글자 찾아 터치 | 지정한 글자와 영역을 확인해 누릅니다.<br>글자가 나타날 때까지 기다리도록 설정할 수 있습니다. |
| 글자가 없을 때 | 뒤로가기·스크롤·새로고침·지정 동작 후 다시 찾도록 구성합니다. |
| 반복 실행 | 전체 매크로 반복과 개별 동작 반복을 따로 설정합니다. |
| 실행 제어 | 일시정지·계속 실행·중단을 제공하고, 진행창을 이동하거나 접을 수 있습니다. |
| 선택형 AI | 사용자가 보여 준 글자·그림 시범으로 작은 행동 모델을 학습합니다.<br>기본값은 꺼짐입니다. |
| 응답 지연 처리 | 이미 보낸 터치를 중복 전송하지 않고 결과를 기다리며, 화면을 읽지 못한 오류와 글자 없음을 구분합니다. |

AI는 등록한 시범에서 행동을 선택하는 오프라인 기능입니다. 모든 화면의 의미를 이해하는 범용 언어 모델은 아닙니다.

<a id="quick-start"></a>
## 빠른 시작

1. 24차 공용 APK를 Android 기기나 64비트 에뮬레이터에 설치합니다.
2. 앱 안내에 따라 접근성과 화면 위 표시 권한을 설정합니다.
3. 찾을 글자·탐지 영역·실행할 매크로를 정합니다.
4. 반복 횟수와 글자가 없을 때의 동작을 선택한 뒤 시작합니다.
5. 그림 시범 학습이 필요할 때만 설정에서 AI 기능을 켭니다.

자세한 조작은 [사용 안내](최종/사용안내.md), 설치 파일 정보는 [최종 안내](최종/README.md)에서 확인합니다. **APK와 ZIP은 로컬 최종 폴더에 제공되며, 이 저장소에는 설치 파일이 업로드되어 있지 않습니다.**

## 확인한 결과

| 항목 | 24차 결과 |
| --- | --- |
| 단위 검사 | 44개 클래스·280개 통과 |
| 린트 | 오류 0개·경고 4개 |
| 전용 기능 검사 | 한 Watch 사이클에서 동작 15회·완료 1회 |
| 전용 시각 검사 | 2개 통과 |
| 설치 파일 | 버전 24/2.4.0·공용 APK 1개 |

전용 시험의 15 동작은 전체 매크로를 15바퀴 실행했다는 뜻이 아닙니다. 시험 시간·최종 파일 지문·실제 기기 시험과 최종 빌드의 관계는 [24차 검증 결과](24차/검증_결과.md)에 모았습니다. 실제 게임의 처리 시간, 모든 기기의 인식률, 장기 무중단 운용은 측정하지 않았습니다.

<a id="input-processing-output"></a>
## 실행 흐름

| 입력 | 처리 | 결과 |
| --- | --- | --- |
| 동작 목록·찾을 글자·탐지 영역 | 화면 정보 또는 한글 OCR로 조건 확인 | 현재 화면의 글자 터치·대기·분기 |
| 사용자가 확정한 시범 | 글자·화면 색 배치·이미지 조각으로 행동 학습 | 등록한 행동 선택과 현재 위치 탐색 |

<a id="repository-structure"></a>
## 소스와 문서

| 자료 | 위치 |
| --- | --- |
| 현재 제품 소스 | [24차/source](24차/source) |
| 설치·설정·조작 | [24차 사용 안내](24차/사용안내.md) |
| 결과와 확인 범위 | [24차 검증 결과](24차/검증_결과.md) |
| 차수별 설명 | [전체 차수 목차](docs/REVISION_INDEX.md) |
| 문서 작성 기준 | [차수별 문서 기준](docs/차수별_문서작성기준.md) |

## 전체 차수

각 차수의 설명은 그 차수의 기능·설정·결과를 다룹니다. 이전 차수와의 비교는 각 문서의 **바뀐 점·차이점**에서 확인합니다.

<details>
<summary>전체 차수와 자료 펼치기</summary>

| 차수 | 핵심 변경 | 자료 |
| --- | --- | --- |
| 기본 | NEXSource2의 편집·서비스·제어 소스와 NEXSource3의 일부 wrapper 는 완전성 범위가 다릅니다. | [자료 폴더](코드) · [같은 수준 상세](docs/versions/기본.md#reading-level-5) · [전체 비교](docs/revision-audit/기본.md) |
| 1차 | MainActivity의 편집, Store의 JSON 저장, Step의 검증, MacroService의 제스처가 이어집니다. | [자료 폴더](1차) · [같은 수준 상세](docs/versions/1차.md#reading-level-5) · [전체 비교](docs/revision-audit/1차.md) |
| 2차 | 글자 동작·OCR 미리 보기 JSON과 사용법·당시 검증 기록을 대조합니다. | [자료 폴더](2차) · [같은 수준 상세](docs/versions/2차.md#reading-level-5) · [전체 비교](docs/revision-audit/2차.md) |
| 3차 | FloatingController와 GesturePaths가 별도 제어 창·녹화·곡선·다중 터치 구성으로 연결됩니다. | [자료 폴더](3차) · [같은 수준 상세](docs/versions/3차.md#reading-level-5) · [전체 비교](docs/revision-audit/3차.md) |
| 4차 | 새로 고침·글자 탐지·선택 매크로·복귀가 Watch 흐름이며 영역과 알림 아이콘도 바뀝니다. | [자료 폴더](4차) · [같은 수준 상세](docs/versions/4차.md#reading-level-5) · [전체 비교](docs/revision-audit/4차.md) |
| 5차 | 탐지 기본 15회는 저장 매크로 전체 반복과 곱하지 않고 WatchConfig와 원본을 분리합니다. | [자료 폴더](5차) · [같은 수준 상세](docs/versions/5차.md#reading-level-5) · [전체 비교](docs/revision-audit/5차.md) |
| 6차 | 최대 6개 비중첩 우선 영역, 대상 앱, 중복 잠금과 완료 후 대기를 구성하고 임시 녹화로 준비합니다. | [자료 폴더](6차) · [같은 수준 상세](docs/versions/6차.md#reading-level-5) · [전체 비교](docs/revision-audit/6차.md) |
| 7차 | 녹스 5개 동 시 실행을 준비했지만 당시 프로그램 미발견·공간 조건으로 실행을 시작하지 않았습니다. | [자료 폴더](7차) · [같은 수준 상세](docs/revision-audit/7차.md#reading-level-5) · [전체 비교](docs/revision-audit/7차.md) |
| 8차 | QueryLane의 화면 읽기와 ReaderResponsivenessTest의 응답 경로를 연결해 봅니다. | [자료 폴더](8차) · [같은 수준 상세](docs/versions/8차.md#reading-level-5) · [전체 비교](docs/revision-audit/8차.md) |
| 9차 | 설정 없는 플로팅 재생은 시작하지 않고 CapturePacer·CaptureRecoveryTest가 늦은 캡처와 오류를 다룹니다. | [자료 폴더](9차) · [같은 수준 상세](docs/versions/9차.md#reading-level-5) · [전체 비교](docs/revision-audit/9차.md) |
| 10차 | MainActivity의 하단 탐색·카드 UI와 PXE 명칭의 변경을 원래 동작과 구분합니다. | [자료 폴더](10차) · [같은 수준 상세](docs/versions/10차.md#reading-level-5) · [전체 비교](docs/revision-audit/10차.md) |
| 11차 | AUTO에서 접근성 읽기가 늦으면 OCR로 전환하는 경로와 순수 회귀·UTF-8·기기 도구를 함께 다룹니다. | [자료 폴더](11차) · [같은 수준 상세](docs/versions/11차.md#reading-level-5) · [전체 비교](docs/revision-audit/11차.md) |
| 12차 | 글자 터치·대기의 계속 기다리기와 이전 실행·일시 정지 뒤 도착한 응답의 수명을 구분합니다. | [자료 폴더](12차) · [같은 수준 상세](docs/versions/12차.md#reading-level-5) · [전체 비교](docs/revision-audit/12차.md) |
| 13차 | 접힌 패널 정지·연결 종료, 미완료 캡처 최대 2개와 실제 OCR Task 하나를 관리합니다. | [자료 폴더](13차) · [같은 수준 상세](docs/versions/13차.md#reading-level-5) · [전체 비교](docs/revision-audit/13차.md) |
| 14차 | 청 록 UI·48dp·모션 설정과 TEXT_TAP 글자 소실 시 원래 waitForever 대기로 돌아가는 흐름이 바뀝니다. | [자료 폴더](14차) · [같은 수준 상세](docs/versions/14차.md#reading-level-5) · [전체 비교](docs/revision-audit/14차.md) |
| 15차 | 계속 기다리기·인식 후 추가 대기 생략·목록 즉시 진입은 적용 범위가 다릅니다.<br>즉시 진입은 기본 꺼짐입니다. | [자료 폴더](15차) · [같은 수준 상세](docs/versions/15차.md#reading-level-5) · [전체 비교](docs/revision-audit/15차.md) |
| 16차 | 기본 꺼짐 adaptiveWait는 같은 인식 방식·영역의 인접 조건에서 다음 글자 사전 부재가 확인될 때만 적용됩니다. | [자료 폴더](16차) · [같은 수준 상세](docs/versions/16차.md#reading-level-5) · [전체 비교](docs/revision-audit/16차.md) |
| 17차 | 조회 하나·새 화면 재확인·완료 OCR 재시도와 0.25~2초 간격을 유지하고 실제 연결 해제를 구분합니다. | [자료 폴더](17차) · [같은 수준 상세](docs/versions/17차.md#reading-level-5) · [전체 비교](docs/revision-audit/17차.md) |
| 18차 | 실제로 완료한 뒤 수를 올려7/15 완료와 8회째 실행 중을 나누고 단계·색 이외의 상태 글자를 표시합니다. | [자료 폴더](18차) · [같은 수준 상세](docs/versions/18차.md#reading-level-5) · [전체 비교](docs/revision-audit/18차.md) |
| 19차 | 오래된 좌표·불필요한 뒤로가기·중복 해제를 막고 대상 앱을 새로 확인하며 앞선 완료 횟수를 유지합니다. | [자료 폴더](19차) · [같은 수준 상세](docs/versions/19차.md#reading-level-5) · [전체 비교](docs/revision-audit/19차.md) |
| 실행 점검-2차 | 보존된 캡처 도구의 PNG CRC·폭·높이·SHA와 명령 기록을 확인합니다. | [자료 폴더](실행점검/2차) · [같은 수준 상세](docs/versions/실행점검-2차.md#reading-level-5) · [전체 비교](docs/revision-audit/실행점검-2차.md) |
| 최종.json | 글자·좌표·재시도·대기·스크롤 조건을 가진 매크로 자료를 읽습니다. | [자료 폴더](docs/materials/022-최종설정) · [원본 파일](최종.json) · [같은 수준 상세](docs/materials/022-최종설정/README.md#reading-level-5) · [전체 비교](docs/versions/최종설정.md) |
| 고유 작업 자료 | ZIP 경로·링크·암호화·크기 검토와 동일 바이트 또는 BOM·개행 대응을 구분하고 고유 소스를 보존합니다. | [자료 폴더](보존자료/작업소스) · [같은 수준 상세](docs/SOURCE_MATERIALS.md#reading-level-5) · [전체 비교](docs/REVISION_AUDIT.md) |
| 20차·시험 도구 | OCR·15회 반복·부재와 재등장·중복 차단을 관측하며 호스트와 장치 시간·세대를 대조합니다. | [자료 폴더](20차) · [같은 수준 상세](docs/versions/20차.md#reading-level-5) · [전체 비교](docs/revision-audit/20차.md) |
| 21차 | 확인 글자 부재·되돌아갈 글자 우선·새 글자로 처음부터 시작·이전 세대 늦은 응답 차 단을 연결합니다. | [자료 폴더](21차) · [같은 수준 상세](docs/versions/21차.md#reading-level-5) · [전체 비교](docs/revision-audit/21차.md) |
| 22차 | 작은 진행 창 이동·접기와 대기 흐름을 보강하고 공개 소스와 운영 자료를 분리합니다. | [자료 폴더](22차) · [같은 수준 상세](docs/versions/22차.md#reading-level-5) · [전체 비교](docs/revision-audit/22차.md) |
| 23차 | OCR 584 → 32 → K의 F64 MLP·시범 2·학습 192·자동 터치 1·종료 뒤 입력 0·가져온 뒤 64를 버전에 결합합니다. | [자료 폴더](23차) · [같은 수준 상세](docs/versions/23차.md#reading-level-5) · [전체 비교](docs/revision-audit/23차.md) |
| 24차 | 1160차원 OCR +RGB 정책·구형 584호환·동일 future 대기·제어창수리와 정상 빌드 13의 단위 44 클래스 280개 통과를 구분합니다. | [자료 폴더](24차) · [같은 수준 상세](docs/versions/24차.md#reading-level-5) · [전체 비교](docs/revision-audit/24차.md) |
| 최종 | 24차 공용 APK·ZIP·사용 안내입니다. | [자료 폴더](최종) · [같은 수준 상세](최종/README.md#reading-level-5) · [전체 비교](docs/revision-audit/최종.md) |

</details>

<a id="-23차--선택형-오프라인-시범-학습"></a>

<a id="implementation-details"></a>

<a id="-편집인식제스처저장-모듈"></a>

<a id="overview"></a>

<a id="latest-changes"></a>

<a id="-이전-14차의-변화와-차수-탐색"></a>

<a id="-차수-바로가기"></a>

<a id="preview"></a>

<a id="-입력처리출력"></a>

<a id="introduction"></a>

<a id="-프로젝트-소개"></a>

<a id="-저장소-구성"></a>

<a id="implementation"></a>

<a id="-주요-기술과-실행-흐름"></a>

<a id="-저장인식-요청의-경계"></a>

<a id="️-차수별로-달라진-동작"></a>

<a id="-기록에-남는-정보"></a>

<a id="-빠른-시작"></a>

<a id="data-settings"></a>

<a id="-데이터와-동작-구성"></a>

<a id="troubleshooting"></a>

<a id="️-문제-해결"></a>

<a id="-모든-차수보관본과-전체-차이"></a>

<a id="️-차수별-화면과-검증-이미지"></a>
