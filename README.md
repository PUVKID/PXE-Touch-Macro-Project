<a name="-pxe-project"></a>


추가 운용 검증: [실제 OCR 75회와 진입·15회·복귀 5바퀴](실행점검/26차_연속운용/README.md) — 제품 APK 변경 없이 새로운 기기 검사 2개를 추가했습니다.
<!-- readme-header32:start -->
<a name="pxe-project"></a>
<p align="center"><img src="assets/readme/icon.svg" alt="" width="72" height="72" /></p>
<h1 align="center" id="project-title">PXE Project</h1>
<p align="center">화면을 읽고 조건에 따라 실행하는 Android 도구</p>
<p align="center">
  <a href="README.md"><img src="assets/readme/language-ko-active.svg" alt="한국어 · 현재 언어" width="126" height="32" /></a>
  <a href="en/README.md"><img src="assets/readme/language-en-link.svg" alt="English" width="126" height="32" /></a>
</p>
<p align="center">
  <a href="docs/읽기안내/레벨1.md"><img src="assets/readme/level-1.svg" alt="LEVEL 1 - Beginner" width="92" height="32" /></a>
  <a href="docs/읽기안내/레벨2.md"><img src="assets/readme/level-2.svg" alt="LEVEL 2 - Explorer" width="92" height="32" /></a>
  <a href="docs/읽기안내/레벨3.md"><img src="assets/readme/level-3.svg" alt="LEVEL 3 - Practitioner" width="92" height="32" /></a>
  <a href="docs/읽기안내/레벨4.md"><img src="assets/readme/level-4.svg" alt="LEVEL 4 - Advanced" width="92" height="32" /></a>
  <a href="docs/읽기안내/레벨5.md"><img src="assets/readme/level-5.svg" alt="LEVEL 5 - Expert" width="92" height="32" /></a>
</p>
<picture>
  <source media="(max-width: 600px) and (prefers-color-scheme: dark)" srcset="assets/readme/flow-ko-dark-mobile.svg" />
  <source media="(max-width: 600px)" srcset="assets/readme/flow-ko-light-mobile.svg" />
  <source media="(prefers-color-scheme: dark)" srcset="assets/readme/flow-ko-dark.svg" />
  <source media="(prefers-color-scheme: light)" srcset="assets/readme/flow-ko-light.svg" />
  <img src="assets/readme/flow-ko-light.svg" alt="화면 글자 · 영역: 사용자가 정한 글자와 화면 영역 → OCR · 조건 판단: 오프라인 한글 OCR로 조건 확인 → 터치 · 매크로 실행: 터치·스크롤·뒤로가기 실행" width="100%" />
</picture>
<!-- readme-header32:end -->

사용자가 정한 글자와 영역을 확인해 터치·스크롤·뒤로가기를 실행하는 Android 매크로입니다.

최신 수정본: 28차 · 2.8.0 · Android 11 이상 · ARM64 / x86_64 · 오프라인 한글 OCR

이전 최종본 26차와 준비본 27차는 별도로 보존합니다.

[사용 안내](28차/사용안내.md) · [28차 상세](docs/versions/28차.md#user-content-reading-level-1) · [전체 소스](28차/source) · [검증 결과](28차/검증_결과.md)

## 읽는 수준 선택

| 수준 | 읽을 내용 |
| --- | --- |
| [LEVEL 1 - Beginner](docs/읽기안내/레벨1.md) | 설치부터 실행까지 쉬운 설명 |
| [LEVEL 2 - Explorer](docs/읽기안내/레벨2.md) | 탐지와 실행의 흐름 |
| [LEVEL 3 - Practitioner](docs/읽기안내/레벨3.md) | 설정·저장·빌드 방법 |
| [LEVEL 4 - Advanced](docs/읽기안내/레벨4.md) | 구현과 오류·취소 처리 |
| [LEVEL 5 - Expert](docs/읽기안내/레벨5.md) | 코드·실행 근거·전체 비교 |

## 28차 · 실행 제어와 작은 진행 창

16차의 매크로·탐지 설정을 유지하면서 모든 탭의 일시정지·계속 실행·정지, 글자 변경 뒤 바로 재시작, 작은 이동식 진행 창을 정리했습니다. 검정·흰색과 파란 포인트, 라이트·다크 모드와 한글 글꼴을 유지하고 3D 장식은 제거했습니다. 학습형 행동 AI는 포함하지 않으며 오프라인 한글 OCR은 유지합니다.

| 자료 | 확인할 내용 |
|---|---|
| [28차 자료](28차) · [사용 안내](28차/사용안내.md) | 글자·영역·매크로 설정과 실행 제어 |
| [28차 상세](docs/versions/28차.md#user-content-reading-level-1) · [전체 비교](docs/revision-audit/28차.md) | 다섯 읽기 수준·구현·변경 범위 |
| [검증 결과](28차/검증_결과.md) · [파일 지문](28차/파일정보.json) | 단위 102개·린트 오류/경고 0·Android12 검사 11개 |
| [작은 진행 창](28차/화면_확인/28_run_compact.png) · [펼친 상세](28차/화면_확인/28_run_expanded.png) | 실제 시험 장치의 화면, 생성 시안 아님 |

실제 시험 화면에서 목록 진입 → OCR 동작 15회 → 뒤로가기 → 재탐지를 5바퀴 진행해 터치 75회·진입 5회·뒤로가기 5회를 확인했습니다. 정지 후 관찰을 포함한 총 경과는 65.9초입니다. 같은 조건의 이전 차수와 비교한 속도 향상 수치나 게임 완료 시간이 아니며, 장기·모든 환경의 무오류를 뜻하지 않습니다.

Rust는 Windows 빌드 시 실제 화면 색상 XML을 생성합니다. Android 런타임 OCR 엔진을 Rust로 교체한 것은 아닙니다. APK와 전체 소스 ZIP은 로컬 28차에 두고 공개 저장소에는 소스·설명·실제 화면·바이너리 지문을 반영합니다.

## 27차 준비본 · 속도와 화면 정리

기존 매크로와 오프라인 OCR을 유지하고 캡처 대기·상태 표시·라이트/다크 색상을 정리했습니다. **기본 최종본은 26차를 유지**합니다. 27차의 60분 검사는 호스트 재부팅으로 종료 근거가 없어 완료 판정을 보류했습니다.

| 자료 | 확인한 내용 |
|---|---|
| [27차 자료](27차) · [사용 안내](27차/사용안내.md) | 공용 APK와 전체 패키지는 로컬 27차 폴더에서 제공 |
| [27차 검증 결과](27차/검증_결과.md) | 단위 106개·변경 영향 기기 검사·별도 속도 비교, 장기 판정 미완료 |
| [27차 상세](docs/versions/27차.md#user-content-reading-level-1) · [전체 비교](27차/전체변경_분할/안내.md) | 같은 수준 설명·실제 추가/수정/삭제·원값 |

전용 Android12의 OCR 터치 15회×5구간에서 평균 10.2초 → 9.1초로 **11.2% 감소**했습니다. 게임 전체 매크로의 완료 시간이나 모든 환경의 정확도를 의미하지 않습니다.

## 26차 · 16차 기반 경량 인식

| 기능 | 현재 동작 |
| --- | --- |
| 글자 인식 | 오프라인 한글 OCR·화면 정보, Unicode 공백 비교와 빈 검색어 오일치 방지 |
| 일반 매크로 | 녹화·터치·제스처·대기·조건 분기·뒤로가기·반복·저장 |
| 목록 탐지 | 최대 6순위·반복 횟수·중복 방지·완료 후 대기·새로고침 |
| 실행 제어 | Ⅱ/▶·정지·이동·접기·투명도·진행 시간 |
| 화면 | 16차의 라이트·다크와 기존 디자인 유지 |
| 느린 창 응답 | 조회 누적 없이 대기하고 이전 응답 폐기. 유한 설정·정지·실제 오류 구분 유지 |
| 별도 AI | 시범 학습·행동 모델·ONNX 기능은 포함하지 않음. OCR은 유지 |

## 확인한 결과

| 항목 | 26차 결과 |
| --- | --- |
| 단위·린트 | **106개 통과 · 린트 오류 0 · 경고 0** |
| 최종 축소 APK 기기 검사 | **89개 통과** · 전용 Android 12 가상 장치 |
| 설치 파일 | **32,717,854바이트** · 두 ABI 포함 · 기존 서명 유지 |
| PC용 16차와 크기 비교 | **7.4% 감소** · 속도·인식률을 뜻하지 않음 |

실제 사용자 블루스택·게임·노트10+나 6개 동시 운용은 이번에 조작·측정하지 않았습니다. 기능별 근거와 미확인 범위를 [검증 결과](26차/검증_결과.md)에 구분합니다. APK와 전체 패키지 ZIP은 로컬 26차·최종 폴더에 제공하며 GitHub에는 바이너리를 올리지 않습니다.

## 빠른 시작

1. 공용 APK를 기존 앱 위에 업데이트로 설치하고 접근성·화면 위 표시 권한을 설정합니다.
2. 안내에 따라 글자·영역·매크로를 정하고 이름으로 저장합니다.
3. 탐지할 매크로·반복·우선 영역·복귀와 새로고침 시간을 정합니다.
4. 시작 후 Ⅱ/▶와 정지, 진행·오류 기록으로 제어합니다.

<a id="current-24-final"></a>
[이전 24차 설명](docs/versions/24차.md) · [차수별 문서 기준](docs/차수별_문서작성기준.md)

[전체 차수 목차](docs/REVISION_INDEX.md)

## 전체 차수

각 차수의 설명은 그 차수의 기능·설정·결과를 다룹니다. 이전 차수와의 비교는 각 문서의 **바뀐 점·차이점**에서 확인합니다.

<details>
<summary>전체 차수와 자료 펼치기</summary>

| 차수 | 핵심 변경 | 자료 |
| --- | --- | --- |
| 기본 | NEXSource2의 편집·서비스·제어 소스와 NEXSource3의 일부 wrapper는 완전성 범위가 다릅니다. | [자료 폴더](코드) · [같은 수준 상세](docs/versions/기본.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/기본.md) |
| 1차 | MainActivity의 편집, Store의 JSON 저장, Step의 검증, MacroService의 제스처가 이어집니다. | [자료 폴더](1차) · [같은 수준 상세](docs/versions/1차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/1차.md) |
| 2차 | 글자 동작·OCR 미리 보기 JSON과 사용법·당시 검증 기록을 대조합니다. | [자료 폴더](2차) · [같은 수준 상세](docs/versions/2차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/2차.md) |
| 3차 | FloatingController와 GesturePaths가 별도 제어 창·녹화·곡선·다중 터치 구성으로 연결됩니다. | [자료 폴더](3차) · [같은 수준 상세](docs/versions/3차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/3차.md) |
| 4차 | 새로 고침·글자 탐지·선택 매크로·복귀가 Watch 흐름이며 영역과 알림 아이콘도 바뀝니다. | [자료 폴더](4차) · [같은 수준 상세](docs/versions/4차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/4차.md) |
| 5차 | 탐지 기본 15회는 저장 매크로 전체 반복과 곱하지 않고 WatchConfig와 원본을 분리합니다. | [자료 폴더](5차) · [같은 수준 상세](docs/versions/5차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/5차.md) |
| 6차 | 최대 6개 비중첩 우선 영역, 대상 앱, 중복 잠금과 완료 후 대기를 구성하고 임시 녹화로 준비합니다. | [자료 폴더](6차) · [같은 수준 상세](docs/versions/6차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/6차.md) |
| 7차 | 녹스 5개 동시 실행을 준비했지만 당시 프로그램 미발견·공간 조건으로 실행을 시작하지 않았습니다. | [자료 폴더](7차) · [같은 수준 상세](docs/revision-audit/7차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/7차.md) |
| 8차 | QueryLane의 화면 읽기와 ReaderResponsivenessTest의 응답 경로를 연결해 봅니다. | [자료 폴더](8차) · [같은 수준 상세](docs/versions/8차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/8차.md) |
| 9차 | 설정 없는 플로팅 재생은 시작하지 않고 CapturePacer·CaptureRecoveryTest가 늦은 캡처와 오류를 다룹니다. | [자료 폴더](9차) · [같은 수준 상세](docs/versions/9차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/9차.md) |
| 10차 | MainActivity의 하단 탐색·카드 UI와 PXE 명칭의 변경을 원래 동작과 구분합니다. | [자료 폴더](10차) · [같은 수준 상세](docs/versions/10차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/10차.md) |
| 11차 | AUTO에서 접근성 읽기가 늦으면 OCR로 전환하는 경로와 순수 회귀·UTF-8·기기 도구를 함께 다룹니다. | [자료 폴더](11차) · [같은 수준 상세](docs/versions/11차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/11차.md) |
| 12차 | 글자 터치·대기의 계속 기다리기와 이전 실행·일시 정지 뒤 도착한 응답의 수명을 구분합니다. | [자료 폴더](12차) · [같은 수준 상세](docs/versions/12차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/12차.md) |
| 13차 | 접힌 패널 정지·연결 종료, 미완료 캡처 최대 2개와 실제 OCR Task 하나를 관리합니다. | [자료 폴더](13차) · [같은 수준 상세](docs/versions/13차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/13차.md) |
| 14차 | 청록 UI·48dp·모션 설정과 TEXT_TAP 글자 소실 시 원래 waitForever 대기로 돌아가는 흐름이 바뀝니다. | [자료 폴더](14차) · [같은 수준 상세](docs/versions/14차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/14차.md) |
| 15차 | 계속 기다리기·인식 후 추가 대기 생략·목록 즉시 진입은 적용 범위가 다릅니다.<br>즉시 진입은 기본 꺼짐입니다. | [자료 폴더](15차) · [같은 수준 상세](docs/versions/15차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/15차.md) |
| 16차 | 기본 꺼짐 adaptiveWait는 같은 인식 방식·영역의 인접 조건에서 다음 글자 사전 부재가 확인될 때만 적용됩니다. | [자료 폴더](16차) · [같은 수준 상세](docs/versions/16차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/16차.md) |
| 17차 | 조회 하나·새 화면 재확인·완료 OCR 재시도와 0.25~2초 간격을 유지하고 실제 연결 해제를 구분합니다. | [자료 폴더](17차) · [같은 수준 상세](docs/versions/17차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/17차.md) |
| 18차 | 실제로 완료한 뒤 수를 올려7/15 완료와 8회째 실행 중을 나누고 단계·색 이외의 상태 글자를 표시합니다. | [자료 폴더](18차) · [같은 수준 상세](docs/versions/18차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/18차.md) |
| 19차 | 오래된 좌표·불필요한 뒤로가기·중복 해제를 막고 대상 앱을 새로 확인하며 앞선 완료 횟수를 유지합니다. | [자료 폴더](19차) · [같은 수준 상세](docs/versions/19차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/19차.md) |
| 실행 점검-2차 | 보존된 캡처 도구의 PNG CRC·폭·높이·SHA와 명령 기록을 확인합니다. | [자료 폴더](실행점검/2차) · [같은 수준 상세](docs/versions/실행점검-2차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/실행점검-2차.md) |
| 최종.json | 글자·좌표·재시도·대기·스크롤 조건을 가진 매크로 자료를 읽습니다. | [자료 폴더](docs/materials/022-최종설정) · [원본 파일](최종.json) · [같은 수준 상세](docs/materials/022-최종설정/README.md#user-content-reading-level-5) · [전체 비교](docs/versions/최종설정.md) |
| 고유 작업 자료 | ZIP 경로·링크·암호화·크기 검토와 동일 바이트 또는 BOM·개행 대응을 구분하고 고유 소스를 보존합니다. | [자료 폴더](보존자료/작업소스) · [같은 수준 상세](docs/SOURCE_MATERIALS.md#user-content-reading-level-5) · [전체 비교](docs/REVISION_AUDIT.md) |
| 20차·시험 도구 | OCR·15회 반복·부재와 재등장·중복 차단을 관측하며 호스트와 장치 시간·세대를 대조합니다. | [자료 폴더](20차) · [같은 수준 상세](docs/versions/20차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/20차.md) |
| 21차 | 확인 글자 부재·되돌아갈 글자 우선·새 글자로 처음부터 시작·이전 세대 늦은 응답 차단을 연결합니다. | [자료 폴더](21차) · [같은 수준 상세](docs/versions/21차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/21차.md) |
| 22차 | 작은 진행 창 이동·접기와 대기 흐름을 보강하고 공개 소스와 운영 자료를 분리합니다. | [자료 폴더](22차) · [같은 수준 상세](docs/versions/22차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/22차.md) |
| 23차 | OCR 584 → 32 → K의 F64 MLP·시범 2·학습 192·자동 터치 1·종료 뒤 입력 0·가져온 뒤 64를 버전에 결합합니다. | [자료 폴더](23차) · [같은 수준 상세](docs/versions/23차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/23차.md) |
| 24차 | 1160차원 OCR +RGB 정책·구형 584호환·동일 future 대기·제어창수리와 정상 빌드 13의 단위 44 클래스 280개 통과를 구분합니다. | [자료 폴더](24차) · [같은 수준 상세](docs/versions/24차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/24차.md) |
| 28차 | 실행 제어·글자 재시작·작은 진행 창, 단위102·기기11개 검사 | [자료 폴더](28차) · [같은 수준 상세](docs/versions/28차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/28차.md) |
| 27차 · 준비본 | 캡처 대기·상태 표시·색상 정리, 장기 판정 미완료 | [자료 폴더](27차) · [같은 수준 상세](docs/versions/27차.md#user-content-reading-level-1) · [전체 비교](docs/revision-audit/27차.md) |
| 26차 | 16차의 기본 매크로·UI를 기준으로 별도 AI를 제외하고 글자 비교·창 응답·축소를 보강합니다. | [자료 폴더](26차) · [같은 수준 상세](docs/versions/26차.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/26차.md) |
| 최종 | 26차 공용 APK·전체 소스 패키지 ZIP·사용 안내입니다. | [자료 폴더](최종) · [같은 수준 상세](최종/README.md#user-content-reading-level-5) · [전체 비교](docs/revision-audit/최종.md) |

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
