<div align="center">

<a name="-최종json--매크로-설정-예제"></a>
# 최종.json · 매크로 설정 예제

schema =1·이름 최종인 7 단계 글자 터치 설정입니다.

**기술 분류:** schema 1 · 매크로 JSON 설정 예제

[핵심 기능](#features) · [사용 흐름](#usage) · [자료 구성](#structure) · [준비와 실행](#setup) · [결과 읽기](#results) · [직전 변경](#changes)

</div>
<!-- reading-levels:start -->
[LEVEL 1 - Beginner](#user-content-reading-level-1) · [LEVEL 2 - Explorer](#user-content-reading-level-2) · [LEVEL 3 - Practitioner](#user-content-reading-level-3) · [LEVEL 4 - Advanced](#user-content-reading-level-4) · [LEVEL 5 - Expert](#user-content-reading-level-5)

프로젝트 전체의 설명은 [LEVEL 1 - Beginner](../../%EC%9D%BD%EA%B8%B0%EC%95%88%EB%82%B4/%EB%A0%88%EB%B2%A81.md)부터 [LEVEL 5 - Expert](../../%EC%9D%BD%EA%B8%B0%EC%95%88%EB%82%B4/%EB%A0%88%EB%B2%A85.md)까지 같은 범위로 연결됩니다.

전체 상세와 같은 수준 탐색: [LEVEL 5 프로젝트](../../읽기안내/레벨5.md) · [LEVEL 5 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨5.md)

<details open>
<summary>LEVEL 1 · Beginner — 목적과 동작부터</summary>

<a id="reading-level-1"></a>

## LEVEL 1 - Beginner · 최종.json

[LEVEL 2](#user-content-reading-level-2) · [LEVEL 3](#user-content-reading-level-3) · [LEVEL 4](#user-content-reading-level-4) · [LEVEL 5](#user-content-reading-level-5)

[같은 수준 프로젝트](../../읽기안내/레벨1.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨1.md) · [프로젝트 첫 화면](../../../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/materials/022-%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95/README.md#reading-level-1)

### LEVEL 1 · 자료의 목적·입력과 확인 순서

최종.json은 **이름이 ‘최종’인 매크로 설정 예제**입니다. 최신 앱이나 최종 납품 제품이라는 뜻이 아닙니다. JSON은 항목 이름과 값을 짝 지은 저장 형식이고 schema =1은 그 형식의 버전입니다. 7개 TEXT_TAP(글자 찾아 터치) 동작을 담습니다.

UTF-8 JSON 편집기로 열람할 수 있으며 빌드 도구·서명은 필요 없습니다.

지원 앱의 불러오기 화면에서는 schema =1, startAt =1(시작 번호), rounds =1(전체 반복), gap =1000, startDelay =0, maxMinutes =30을 검토합니다. 7개 동작의 문구와 실패 행동을 실제 화면과 대조합니다.

CONTAINS는 부분 일치, AUTO는 자동 인식이며 ignoreSpaces·ignoreCase와 전체 화면 검색 범위를 확인합니다.

첫 동작 실패 정책은 SCROLL_RETRY(스크롤 뒤 재시도), 나머지는 RETRY(재시도)입니다. timeout은 첫 1000·이후 3000, retries =3, retryDelay =1000입니다.

텍스트·좌표·재시도·대기·스크롤 재시도 값이 있다는 것은 실행 조건을 저장했다는 뜻입니다.

실제 인식·터치·제출 성공이나 APK 버전 기록은 없습니다. 지원 앱에서 의미를 확인한 뒤 사용 여부를 결정합니다.

이 안내 폴더는 원본을 이동하거나 실행 코드를 복제한 것이 아닙니다.

### LEVEL 1 · 원문이 연결한 파일과 구성

아래는 이 자료에서 실제로 연결한 파일과 폴더입니다.

README·사용법은 읽는 안내, JSON은 설정이나 구조화된 기록, 검증 파일은 당시 확인 범위, SHA256은 파일을 식별하는 값입니다.

라이선스 파일은 글꼴 등 자료의 사용 조건이므로 함께 보존합니다.

파일 이름만으로 현재 설치 여부나 새 검사 성공을 판단하지 마십시오.

| 경로 | 자료 종류 |
|---|---|
| [최종.json](../../../%EC%B5%9C%EC%A2%85.json) | 자료 원문 |

### LEVEL 1 · 보존 범위와 전체 비교

전체 비교는 무엇이 추가·수정·삭제됐는지 보여 주는 기록입니다.

아래 ‘+’는 늘어난 행, ‘−’는 없어진 행이며 기능 개수나 성능 점수가 아닙니다.

주석·공백·줄바꿈도 차이에 포함됩니다.

실행 코드처럼 줄로 비교하기 어려운 파일은 크기와 해시로 같은 파일인지 확인합니다.

설명에 없는 과거 차수나 측정 결과를 채워 넣지 않습니다.

[전체 비교·자료 범위](../../versions/최종설정.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

[원본 파일 직접 열기](../../../%EC%B5%9C%EC%A2%85.json) · [같은 수준 상세](#user-content-reading-level-1)



</details>

<details>
<summary>LEVEL 2 · Explorer — 구성과 연결 원리</summary>

<a id="reading-level-2"></a>

## LEVEL 2 - Explorer · 최종.json

[LEVEL 1](#user-content-reading-level-1) · [LEVEL 3](#user-content-reading-level-3) · [LEVEL 4](#user-content-reading-level-4) · [LEVEL 5](#user-content-reading-level-5)

[같은 수준 프로젝트](../../읽기안내/레벨2.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨2.md) · [프로젝트 첫 화면](../../../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/materials/022-%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95/README.md#reading-level-2)

### LEVEL 2 · 자료의 목적·입력과 확인 순서

이 자료의 출력은 실행 결과가 아니라 지원 앱에서 해석할 설정 값입니다. schema1·이름 최종·7개 TEXT_TAP이 구조이고, 대상 문구/부분 일치/자동 인식/좌표/재시도/대기/스크롤 재시도가 행동의 조건을 연결합니다. 설정 이름과 제품 버전은 관계가 없습니다.

상위 설정 schema =1/startAt =1/rounds =1/gap =1000/startDelay =0/maxMinutes =30과 각 동작의 text/failure를 분리해 읽습니다.

전 동작의 CONTAINS·AUTO·ignoreSpaces·ignoreCase·전체 화면 범위는 무엇을 어떤 방식으로 찾는 지 정합니다.

첫 동작 SCROLL_RETRY와 이후 RETRY는 부재 시 후속 행동이 다르고 timeout1000/3000·retries3·retryDelay1000 이 시간/재시도 조건입니다.

UTF-8 편집기 열람에는 빌드·서명이 필요 없으며 schema 지원 앱에서만 불러오기 의미를 대조합니다. **정적 입력과 실제 성공 기록의 경계**를 유지하여 인식/터치/제출 성공·APK 버전을 만들지 않습니다. 원본을 옮기거나 제품 코드를 복제한 폴더도 아닙니다.

### LEVEL 2 · 원문이 연결한 파일과 구성

자료 목록은 기능 설명을 원본에 연결합니다. 안내문·설정 예제·검증 기록·파일 식별 정보가 각각 사용 방법, 입력 형태, 관측 범위, 동일 파일 여부를 보여 줍니다. 폴더에 자료가 있다는 것과 모든 빌드 구성요소가 준비됐다는 것은 별개입니다. 아래 실제 목록을 기준으로 필요한 연결을 따라갑니다.

| 경로 | 자료 종류 |
|---|---|
| [최종.json](../../../%EC%B5%9C%EC%A2%85.json) | 자료 원문 |

### LEVEL 2 · 보존 범위와 전체 비교

변경 설명과 파일 비교는 역할이 다릅니다. 설명은 변화의 의미를, 전체 비교는 추가·수정·삭제와 주석·공백·개행까지의 근거를 제공합니다. 행 증감은 구현량이나 품질 지표로 환산할 수 없습니다. 바이너리는 크기·해시를 통해 동일성을 확인하고 실제 이전 자료가 확인된 범위만 연결합니다.

[전체 비교·자료 범위](../../versions/최종설정.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

[원본 파일 직접 열기](../../../%EC%B5%9C%EC%A2%85.json) · [같은 수준 상세](#user-content-reading-level-2)



</details>

<details>
<summary>LEVEL 3 · Practitioner — 준비·실행·결과 확인</summary>

<a id="reading-level-3"></a>

## LEVEL 3 - Practitioner · 최종.json

[LEVEL 1](#user-content-reading-level-1) · [LEVEL 2](#user-content-reading-level-2) · [LEVEL 4](#user-content-reading-level-4) · [LEVEL 5](#user-content-reading-level-5)

[같은 수준 프로젝트](../../읽기안내/레벨3.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨3.md) · [프로젝트 첫 화면](../../../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/materials/022-%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95/README.md#reading-level-3)

### LEVEL 3 · 자료의 목적·입력과 확인 순서

schema =1·이름 최종·7개 TEXT_TAP을 확인합니다. 열람 자체는 UTF-8 JSON 편집기를 사용하며 빌드/서명 준비와 구분합니다. schema를 지원하는 앱에서만 불러오기 의미를 대조하십시오.

검토 순서는 상위 값 schema =1/startAt =1/rounds =1/gap =1000/startDelay =0/maxMinutes =30 →7개 text/failure의 실제 대상 화면 대조 →CONTAINS/AUTO/ignoreSpaces/ignoreCase/전체 화면 영역 →불러오기 화면에서 사용 여부 결정입니다.

첫 SCROLL_RETRY와 나머지 RETRY, timeout 첫 1000/이후 3000·retries3·retryDelay1000을 혼동하지 마십시오.

텍스트·좌표·대기·재시도·스크롤 재시도 필드도 확인합니다.

**산출물은 매크로 설정값**입니다. 실제 인식/터치/제출 성공이나 APK 버전 증거가 없으므로 실행 성과로 보고하지 않습니다. 이름 최종을 최신 제품으로 분류하지 않으며 안내 폴더는 원본 이동·실행 코드 복제가 아닙니다.

### LEVEL 3 · 원문이 연결한 파일과 구성

아래 파일 목록에서 안내문을 먼저 읽고 사용할 설정과 검증 기록을 대조하십시오. SHA-256 파일은 대응 APK 등의 동일성 확인용이며 실행 성능의 증거는 아닙니다. 라이선스와 원문 파일을 보존하고 일부 자료가 없는 경우 다른 차수 파일로 임의 대체하지 않습니다.

| 경로 | 자료 종류 |
|---|---|
| [최종.json](../../../%EC%B5%9C%EC%A2%85.json) | 자료 원문 |

### LEVEL 3 · 보존 범위와 전체 비교

수정 범위를 재확인할 때 아래 전체 비교에서 추가·수정·삭제 파일을 열고 주석·공백·개행까지 대조하십시오. 표시된 행 증감과 바이너리 크기·해시를 보존합니다. 기능 설명만으로 모든 파일 변경을 확인했다고 처리하지 않으며 비교 대상이 없는 자료에 이전 번호를 만들어 넣지 않습니다.

[전체 비교·자료 범위](../../versions/최종설정.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

[원본 파일 직접 열기](../../../%EC%B5%9C%EC%A2%85.json) · [같은 수준 상세](#user-content-reading-level-3)



</details>

<details>
<summary>LEVEL 4 · Advanced — 구현·설계·실패 처리</summary>

<a id="reading-level-4"></a>

## LEVEL 4 - Advanced · 최종.json

[LEVEL 1](#user-content-reading-level-1) · [LEVEL 2](#user-content-reading-level-2) · [LEVEL 3](#user-content-reading-level-3) · [LEVEL 5](#user-content-reading-level-5)

[같은 수준 프로젝트](../../읽기안내/레벨4.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨4.md) · [프로젝트 첫 화면](../../../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/materials/022-%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95/README.md#reading-level-4)

### LEVEL 4 · 자료의 목적·입력과 확인 순서

설정 계약과 런 타임 증거를 분리합니다. schema1·이름 최종의 7개 TEXT_TAP은 상위 실행 설정과 개별 텍스트/좌표/부분 일치/AUTO/재시도/대기/스크롤 재시도 필드를 결합합니다. 파일 이름은 제품 배포 식별자가 아닙니다.

상위 startAt1/rounds1/gap1000/startDelay0/maxMinutes30, 전 동작 CONTAINS/ignoreSpaces/ignoreCase/전체 화면 범위와 첫 SCROLL_RETRY·이후 RETRY를 보존합니다. timeout1000대 3000, retries3/retryDelay1000 은 실패 경로 분석의 입력이고 실제 관측 시간으로 바꾸지 않습니다.

원문이 밝히지 않은 대상 기능 의미는 추정하지 않습니다.

UTF-8 편집기 열람에는 빌드 도구·서명이 필요 없고 schema 지원 앱에서 해석을 대조해야 합니다. 인식/터치/제출 성공·APK 버전은 이 정적 JSON에 없습니다. 안내 폴더는 별도 원본 읽기 경로이며 소스 복제나 원본 이동이 아닙니다.

### LEVEL 4 · 원문이 연결한 파일과 구성

아래 경로는 이 설명의 추적 가능한 자료 경계입니다. 소스/설정/검증/파일 식별/라이선스를 구별하여 변경 영향과 재현 입력을 대조합니다. 파일 보존과 실행 검증을 별도 증거로 취급하며 이름만으로 구현의 완전성이나 배포 상태를 추론하지 않습니다.

| 경로 | 자료 종류 |
|---|---|
| [최종.json](../../../%EC%B5%9C%EC%A2%85.json) | 자료 원문 |

### LEVEL 4 · 보존 범위와 전체 비교

전체 비교는 소스·설정·문서의 변경 추적 단위입니다. 추가/수정/삭제와 주석·공백·개행을 포함하고 바이너리는 크기/해시로 보존합니다. 행 증감은 의미상 변경과 일대일 대응하지 않으므로 기능 해석과 분리합니다. 확인된 이전 자료만 비교 기준으로 삼고 자료 공백을 추정으로 메우지 않습니다.

[전체 비교·자료 범위](../../versions/최종설정.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

[원본 파일 직접 열기](../../../%EC%B5%9C%EC%A2%85.json) · [같은 수준 상세](#user-content-reading-level-4)



</details>

<details>
<summary>LEVEL 5 · Expert — 근거·프로토콜·한계 검토</summary>

<a id="reading-level-5"></a>
## LEVEL 5 - Expert

기존의 전체 설명·표·코드·함수 참조·파일별 비교는 아래에 그대로 이어집니다. [자료 폴더](.) · [직전 비교·자료 범위](../../versions/%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95.md) · [이 차수의 원래 전체 상세](#user-content-reading-level-5)
<!-- reading-levels:end -->


[프로젝트](../../../README.md) · [전체 차수](../../REVISION_INDEX.md) · [원본 자료](../../../%EC%B5%9C%EC%A2%85.json)

<a name="-목적과-이-차수의-역할"></a>
## 목적과 이 차수의 역할

schema =1·이름 최종인 7 단계 글자 터치 설정입니다.

<a name="features"></a>

<a name="-핵심-기능과-보존-범위"></a>
## 핵심 기능과 보존 범위

| 항목 | 이 차수에서 확인할 내용 |
| --- | --- |
| 1 | schema =1·이름 최종인 7 단계 글자 터치 설정입니다. |
| 2 | 텍스트·좌표·재시도·대기·스크롤 재시도 필드를 포함합니다. |
| 3 | 파일 이름의 최종은 사용자 매크로 이름이며 최신 앱 버전이나 별도 최종 제품이 아닙니다. |

<a name="-입력처리출력"></a>
## 입력·처리·출력

| 단계 | 내용 |
| --- | --- |
| 입력 | 최종.json의 7개 TEXT_TAP 동작과 실행 설정 |
| 처리 | 대상 문구·부분 일치·자동 인식·재시도·대기 필드 해석 |
| 출력 | 지원 앱에서 검토할 매크로 설정값 |

<a name="usage"></a>

<a name="️-화면과-사용-흐름"></a>
## 화면과 사용 흐름

1. schema =1, startAt =1, rounds =1, gap =1000, startDelay =0, maxMinutes =30을 확인합니다.
2. 7개 동작의 text와 failure를 실제 대상 화면과 비교합니다.
3. 모든 동작의 CONTAINS·AUTO·ignoreSpaces·ignoreCase와 전체 화면 검색 범위를 확인합니다.
4. 지원 앱의 불러오기 화면에서 설정을 검토한 뒤 사용 여부를 결정합니다.

<a name="structure"></a>

<a name="-실제-자료-구성"></a>
## 실제 자료 구성

| 경로 | 자료 종류 |
| --- | --- |
| [최종.json](../../../%EC%B5%9C%EC%A2%85.json) | 자료 원문 |

<a name="setup"></a>

<a name="-설치-환경과-실행사용법"></a>
## 설치 환경과 실행·사용법

UTF-8 JSON 편집기로 엽니다. schema를 지원하는 앱에서만 불러오기 의미를 대조하며 빌드 도구나 서명은 이 JSON 열람에 필요하지 않습니다.

관련 파일을 아래 링크에서 열어 순서대로 대조합니다.

<a name="results"></a>

<a name="-처리-결과와-출력-읽기"></a>
## 처리 결과와 출력 읽기

JSON에는 입력 조건만 들어 있습니다. 실제 인식·터치·제출 성공 기록이나 APK 버전 정보는 없습니다.

<a name="️-제약과-문제-해결"></a>
## 제약과 문제 해결

| 상황 | 확인할 내용 |
| --- | --- |
| 첫 동작과 이후 실패 정책 차이 | 첫 동작은 SCROLL_RETRY, 나머지는 RETRY입니다. |
| 시간 값 해석 | timeout은 첫 동작 1000, 이후 3000; retries =3, retryDelay =1000입니다. |
| 원문 대상과 환경 차이 | 이름 최종은 매크로 이름입니다.<br>실제 대상 앱·화면·기능 의미를 확인합니다. |

<a name="changes"></a>

<a name="-직전-자료와-전체-비교"></a>
## 직전 자료와 전체 비교

이 자료의 보존 범위와 기준 파일을 연결합니다.

[전체 비교·자료 범위](../../versions/%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95.md)

추가·수정·삭제, 주석·공백·개행과 바이너리 크기·해시는 연결된 원래 비교 자료에서 확인합니다.


<a name="-이-자료-안내-폴더의-구성"></a>
## 이 자료 안내 폴더의 구성

이 폴더는 원본 파일을 읽는 독립 안내입니다. 실행 코드 폴더를 복제하거나 원본을 옮기지 않았습니다.

[원본 파일 직접 열기](../../../%EC%B5%9C%EC%A2%85.json) · [기존 설명 주소](../../versions/%EC%B5%9C%EC%A2%85%EC%84%A4%EC%A0%95.md)

</details>

