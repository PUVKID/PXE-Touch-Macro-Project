<div align="center">

<a name="️-원본-제품자료와-압축-소스"></a>
# 원본 제품자료와 압축 소스

기존 모든 차수는 [첫 화면 차수표](../README.md)에서 탐색합니다. 이번에는 코드·제품 설정·사용법·설계·실험 기록·자체 제작 리소스·라이선스를 원본 경로와 함께 보강했습니다. 아래는 기존 공개 기준과 바이트가 다른 제품 자료 346개 목록입니다. 전체 차수의 공개 파일은 각 차수 비교 명세에 별도로 있습니다.

**기술 분류:** 제품 원본 보관 · 코드·설정·사용 문서

[핵심 기능](#features) · [사용 흐름](#usage) · [자료 구성](#structure) · [준비와 실행](#setup) · [결과 읽기](#results) · [직전 변경](#changes)

</div>
<!-- reading-levels:start -->
[LEVEL 1 - Beginner](#reading-level-1) · [LEVEL 2 - Explorer](#reading-level-2) · [LEVEL 3 - Practitioner](#reading-level-3) · [LEVEL 4 - Advanced](#reading-level-4) · [LEVEL 5 - Expert](#reading-level-5)

프로젝트 전체의 설명은 [LEVEL 1 - Beginner](%EC%9D%BD%EA%B8%B0%EC%95%88%EB%82%B4/%EB%A0%88%EB%B2%A81.md)부터 [LEVEL 5 - Expert](%EC%9D%BD%EA%B8%B0%EC%95%88%EB%82%B4/%EB%A0%88%EB%B2%A85.md)까지 같은 범위로 연결됩니다.

전체 상세와 같은 수준 탐색: [LEVEL 5 프로젝트](읽기안내/레벨5.md) · [LEVEL 5 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨5.md)

<a id="reading-level-1"></a>

## LEVEL 1 - Beginner · 고유 작업 자료

[LEVEL 2](#reading-level-2) · [LEVEL 3](#reading-level-3) · [LEVEL 4](#reading-level-4) · [LEVEL 5](#reading-level-5)

[같은 수준 프로젝트](읽기안내/레벨1.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨1.md) · [프로젝트 첫 화면](../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/SOURCE_MATERIALS.md#reading-level-1)

### LEVEL 1 · 자료의 목적·입력과 확인 순서

이 묶음은 여러 시점의 **제품 원본과 압축 소스의 보관 목록**입니다.

코드·제품 설정·사용법·설계·실험 기록·자체 제작 리소스·라이선스를 원래 경로와 함께 연결합니다.

기존 공개 기준과 바이트가 다른 제품 자료 346개 목록이며 전체 차수의 모든 공개 파일 수라는 뜻은 아닙니다.

기존 차수는 첫 화면 차수 표에서 찾습니다.

보존 목록에서 원래 차수/경로를 고르고 코드와 해당 설명을 함께 읽은 뒤 보조 도구와 제품 코드를 구분합니다. 서로 다른 시점의 파일을 한 앱의 같은 시점 설치 환경으로 합치지 않습니다. 검증·캐시 정리·복구·배포 도구는 원래 경로와 기존 증거가 필요할 수 있어 일괄 실행하지 않습니다.

ZIP은 파일을 묶은 압축 형식입니다.

중앙 목록과 경로 탈출·링크·암호화·크기를 검사하고 실제 소스 멤버를 읽었습니다.

경로 탈출은 압축을 풀 때 정한 폴더 밖으로 나가는 경우입니다.

기존 공개 파일과 바이트 또는 BOM/개행만 같은 멤버는 대응 링크로, 고유 소스는 압축 원본 소스로 보존합니다.

BOM은 텍스트 맨 앞 인코딩 표식이고 개 행은 줄 바꿈입니다.

**실행 바이너리·캐시·인증 자료·운영 DB·실 거래 계좌·원 작 추출 표현 자산은 제외**했습니다.

모든 ZIP 멤버 판단은 비공개 검토 명세에, 공개 대응은 아래 JSON에 있습니다.

같은 이름도 시점/경로가 다르면 다른 자료입니다.

원위치·크기·SHA-256(파일 식별 값)으로 보존을 확인하고 차이는 원래 차수·경로·해시·전체 비교로 읽습니다.

단일 배포 제품이 아니므로 한 폴더에서 실행되지 않아도 그 자체로 앱 결함이 아닙니다.

### LEVEL 1 · 원문이 연결한 파일과 구성

아래는 이 자료에서 실제로 연결한 파일과 폴더입니다.

README·사용법은 읽는 안내, JSON은 설정이나 구조화된 기록, 검증 파일은 당시 확인 범위, SHA256은 파일을 식별하는 값입니다.

라이선스 파일은 글꼴 등 자료의 사용 조건이므로 함께 보존합니다.

파일 이름만으로 현재 설치 여부나 새 검사 성공을 판단하지 마십시오.

| 경로 | 자료 종류 |
|---|---|
| [11차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/11%EC%B0%A8) | 하위 자료 폴더 |
| [12차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/12%EC%B0%A8) | 하위 자료 폴더 |
| [13차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/13%EC%B0%A8) | 하위 자료 폴더 |
| [14차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8) | 하위 자료 폴더 |
| [15차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/15%EC%B0%A8) | 하위 자료 폴더 |
| [16차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/16%EC%B0%A8) | 하위 자료 폴더 |
| [work/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/work) | 하위 자료 폴더 |

| 파일 | 원문에 정의된 역할·구성 |
|---|---|
| [14차/work/시험장치_복사.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/work/%EC%8B%9C%ED%97%98%EC%9E%A5%EC%B9%98_%EB%B3%B5%EC%82%AC.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/diagnose_long14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/diagnose_long14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/finish_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/finish_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/implement_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/implement_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/ownership_manifest14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/ownership_manifest14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair1_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair1_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair3_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair3_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair4_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair4_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/test_marker14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/test_marker14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |

### LEVEL 1 · 보존 범위와 전체 비교

전체 비교는 무엇이 추가·수정·삭제됐는지 보여 주는 기록입니다.

아래 ‘+’는 늘어난 행, ‘−’는 없어진 행이며 기능 개수나 성능 점수가 아닙니다.

주석·공백·줄바꿈도 차이에 포함됩니다.

실행 코드처럼 줄로 비교하기 어려운 파일은 크기와 해시로 같은 파일인지 확인합니다.

설명에 없는 과거 차수나 측정 결과를 채워 넣지 않습니다.

[전체 비교·자료 범위](REVISION_AUDIT.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

| 차수·위치 | 추가·변경 자료 수 |
|---|---:|
| 10차 | 11 |
| 11차 | 10 |
| 12차 | 10 |
| 13차 | 12 |
| 14차 | 10 |
| 15차 | 3 |
| 16차 | 8 |
| 1차 | 12 |
| 2차 | 4 |
| 3차 | 9 |
| 4차 | 12 |
| 5차 | 12 |
| 6차 | 10 |
| 7차 | 2 |
| 8차 | 10 |
| 9차 | 9 |
| 보존자료 | 170 |
| 실행점검 | 13 |
| 실행환경점검 | 6 |
| 코드 | 13 |

[파일 전체 명세](source-materials/files.json) · [공개 ZIP 멤버 대응](source-materials/archive-members.json)


<a id="reading-level-2"></a>

## LEVEL 2 - Explorer · 고유 작업 자료

[LEVEL 1](#reading-level-1) · [LEVEL 3](#reading-level-3) · [LEVEL 4](#reading-level-4) · [LEVEL 5](#reading-level-5)

[같은 수준 프로젝트](읽기안내/레벨2.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨2.md) · [프로젝트 첫 화면](../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/SOURCE_MATERIALS.md#reading-level-2)

### LEVEL 2 · 자료의 목적·입력과 확인 순서

보관 단위는 제품 버전 하나가 아니라 출처·시점·원래 경로가 결합된 파일입니다. 코드·설정·사용/설계/실험 문서·자체 리소스·라이선스의 346개는 기존 공개 기준과 바이트가 다른 목록이며 전체 차수 공개 파일은 각 비교 명세에 별도로 있습니다. **보존 목록과 배포 구조를 구분**해야 합니다.

원래 차수/경로 탐색→관련 코드와 해당 차수 설명→보조 도구/제품 코드와 입력 대조 순서입니다. 검증/정리/복구/배포 도구는 경로와 증거에 의존할 수 있어 한 번에 실행하지 않습니다. 서로 다른 시점을 동일 설치 환경으로 합치지 않습니다.

ZIP 검토는 중앙 목록·경로 탈출·링크·암호화·크기 경계와 실제 멤버 읽기를 연결합니다.

기존 공개와 바이트 또는 BOM/개행만 같은 자료는 대응 링크, 고유 소스는 압축 원본 소스로 보존합니다.

바이너리·캐시·실제 인증·운영 DB·실 거래 계좌·원 작 추출 표현 자산은 제외합니다.

**멤버별 판단 전체와 공개 대응 JSON의 범위도 다릅니다.** 전자는 비공개 검토 명세, 후자는 아래 공개 링크에서 확인합니다.

원래 위치·크기·SHA-256으로 동일성을 확인하며 같은 이름만으로 합치지 않습니다.

### LEVEL 2 · 원문이 연결한 파일과 구성

자료 목록은 기능 설명을 원본에 연결합니다. 안내문·설정 예제·검증 기록·파일 식별 정보가 각각 사용 방법, 입력 형태, 관측 범위, 동일 파일 여부를 보여 줍니다. 폴더에 자료가 있다는 것과 모든 빌드 구성요소가 준비됐다는 것은 별개입니다. 아래 실제 목록을 기준으로 필요한 연결을 따라갑니다.

| 경로 | 자료 종류 |
|---|---|
| [11차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/11%EC%B0%A8) | 하위 자료 폴더 |
| [12차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/12%EC%B0%A8) | 하위 자료 폴더 |
| [13차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/13%EC%B0%A8) | 하위 자료 폴더 |
| [14차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8) | 하위 자료 폴더 |
| [15차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/15%EC%B0%A8) | 하위 자료 폴더 |
| [16차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/16%EC%B0%A8) | 하위 자료 폴더 |
| [work/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/work) | 하위 자료 폴더 |

| 파일 | 원문에 정의된 역할·구성 |
|---|---|
| [14차/work/시험장치_복사.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/work/%EC%8B%9C%ED%97%98%EC%9E%A5%EC%B9%98_%EB%B3%B5%EC%82%AC.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/diagnose_long14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/diagnose_long14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/finish_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/finish_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/implement_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/implement_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/ownership_manifest14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/ownership_manifest14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair1_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair1_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair3_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair3_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair4_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair4_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/test_marker14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/test_marker14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |

### LEVEL 2 · 보존 범위와 전체 비교

변경 설명과 파일 비교는 역할이 다릅니다. 설명은 변화의 의미를, 전체 비교는 추가·수정·삭제와 주석·공백·개행까지의 근거를 제공합니다. 행 증감은 구현량이나 품질 지표로 환산할 수 없습니다. 바이너리는 크기·해시를 통해 동일성을 확인하고 실제 이전 자료가 확인된 범위만 연결합니다.

[전체 비교·자료 범위](REVISION_AUDIT.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

| 차수·위치 | 추가·변경 자료 수 |
|---|---:|
| 10차 | 11 |
| 11차 | 10 |
| 12차 | 10 |
| 13차 | 12 |
| 14차 | 10 |
| 15차 | 3 |
| 16차 | 8 |
| 1차 | 12 |
| 2차 | 4 |
| 3차 | 9 |
| 4차 | 12 |
| 5차 | 12 |
| 6차 | 10 |
| 7차 | 2 |
| 8차 | 10 |
| 9차 | 9 |
| 보존자료 | 170 |
| 실행점검 | 13 |
| 실행환경점검 | 6 |
| 코드 | 13 |

[파일 전체 명세](source-materials/files.json) · [공개 ZIP 멤버 대응](source-materials/archive-members.json)


<a id="reading-level-3"></a>

## LEVEL 3 - Practitioner · 고유 작업 자료

[LEVEL 1](#reading-level-1) · [LEVEL 2](#reading-level-2) · [LEVEL 4](#reading-level-4) · [LEVEL 5](#reading-level-5)

[같은 수준 프로젝트](읽기안내/레벨3.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨3.md) · [프로젝트 첫 화면](../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/SOURCE_MATERIALS.md#reading-level-3)

### LEVEL 3 · 자료의 목적·입력과 확인 순서

첫 화면 차수 표와 보존 목록에서 필요한 원래 차수·경로를 찾고 해당 코드와 차수 설명을 함께 읽으십시오. 제품 설정/사용법/설계/실험/자체 리소스/라이선스의 출처를 보존합니다. 346개는 기존 공개 기준과 바이트가 다른 제품 자료 목록이며 각 차수 전체 공개 파일은 별도 비교 명세를 확인합니다.

**여러 시점 자료를 한 배포본으로 빌드하거나 도구를 일괄 실행하지 마십시오.** 검증·캐시 정리·복구·배포 도구의 원 경로/기존 증거/필요 입력부터 대조합니다. 같은 이름 파일도 시점·경로·크기·SHA-256과 전체 비교를 확인합니다.

ZIP의 중앙 목록·경로 탈출·링크·암호화·크기 검사와 실제 소스 읽기 판단을 대조하십시오.

바이트 또는 BOM/개행만 같은 멤버는 기존 대응 링크, 고유 소스는 압축 원본 소스에서 확인합니다.

전체 멤버 판단은 비공개, 공개 대응은 아래 JSON입니다.

**제외한 바이너리·캐시·인증·운영 DB·계좌·원 작 추출 표현 자산을 이 목록에 복구하지 않습니다.** 단일 앱 폴더가 아니라는 한계를 유지합니다.

### LEVEL 3 · 원문이 연결한 파일과 구성

아래 파일 목록에서 안내문을 먼저 읽고 사용할 설정과 검증 기록을 대조하십시오. SHA-256 파일은 대응 APK 등의 동일성 확인용이며 실행 성능의 증거는 아닙니다. 라이선스와 원문 파일을 보존하고 일부 자료가 없는 경우 다른 차수 파일로 임의 대체하지 않습니다.

| 경로 | 자료 종류 |
|---|---|
| [11차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/11%EC%B0%A8) | 하위 자료 폴더 |
| [12차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/12%EC%B0%A8) | 하위 자료 폴더 |
| [13차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/13%EC%B0%A8) | 하위 자료 폴더 |
| [14차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8) | 하위 자료 폴더 |
| [15차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/15%EC%B0%A8) | 하위 자료 폴더 |
| [16차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/16%EC%B0%A8) | 하위 자료 폴더 |
| [work/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/work) | 하위 자료 폴더 |

| 파일 | 원문에 정의된 역할·구성 |
|---|---|
| [14차/work/시험장치_복사.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/work/%EC%8B%9C%ED%97%98%EC%9E%A5%EC%B9%98_%EB%B3%B5%EC%82%AC.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/diagnose_long14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/diagnose_long14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/finish_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/finish_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/implement_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/implement_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/ownership_manifest14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/ownership_manifest14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair1_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair1_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair3_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair3_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair4_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair4_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/test_marker14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/test_marker14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |

### LEVEL 3 · 보존 범위와 전체 비교

수정 범위를 재확인할 때 아래 전체 비교에서 추가·수정·삭제 파일을 열고 주석·공백·개행까지 대조하십시오. 표시된 행 증감과 바이너리 크기·해시를 보존합니다. 기능 설명만으로 모든 파일 변경을 확인했다고 처리하지 않으며 비교 대상이 없는 자료에 이전 번호를 만들어 넣지 않습니다.

[전체 비교·자료 범위](REVISION_AUDIT.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

| 차수·위치 | 추가·변경 자료 수 |
|---|---:|
| 10차 | 11 |
| 11차 | 10 |
| 12차 | 10 |
| 13차 | 12 |
| 14차 | 10 |
| 15차 | 3 |
| 16차 | 8 |
| 1차 | 12 |
| 2차 | 4 |
| 3차 | 9 |
| 4차 | 12 |
| 5차 | 12 |
| 6차 | 10 |
| 7차 | 2 |
| 8차 | 10 |
| 9차 | 9 |
| 보존자료 | 170 |
| 실행점검 | 13 |
| 실행환경점검 | 6 |
| 코드 | 13 |

[파일 전체 명세](source-materials/files.json) · [공개 ZIP 멤버 대응](source-materials/archive-members.json)


<a id="reading-level-4"></a>

## LEVEL 4 - Advanced · 고유 작업 자료

[LEVEL 1](#reading-level-1) · [LEVEL 2](#reading-level-2) · [LEVEL 3](#reading-level-3) · [LEVEL 5](#reading-level-5)

[같은 수준 프로젝트](읽기안내/레벨4.md) · [같은 수준 프로필](https://github.com/PUVKID/PUVKID/blob/main/docs/읽기안내/레벨4.md) · [프로젝트 첫 화면](../README.md)

[교체 전 전체 안내](https://github.com/PUVKID/PXE-Touch-Macro-Project/blob/85e77b0671171fca9475365a623fdd718e26eaf4/docs/SOURCE_MATERIALS.md#reading-level-4)

### LEVEL 4 · 자료의 목적·입력과 확인 순서

출처 보존의 식별키는 원 차수·원 경로·파일 크기·SHA-256이며 동일 이름은 동일 자료의 충분 조건이 아닙니다. 346개는 기존 공개 기준과 바이트가 다른 제품 자료 집합이고 전체 차수의 공개 파일 집합은 별도 비교 명세로 추적합니다.

코드·설정·문서·실험·자체 리소스·라이선스를 동일 시점의 단일 앱으로 조립하지 않습니다.

**아카이브 검토 경계는 중앙 목록/경로 탈출/링크/암호화/크기와 실제 멤버 읽기**입니다.

바이트 동일 또는 BOM·개행만 같은 멤버는 대응 링크로 중복을 설명하고 고유 소스는 압축 원본 소스로 보존합니다.

바이너리·캐시·실제 인증·운영 DB·실 거래 계좌·원 작 추출 표현 자산은 공개 집합에서 제외합니다.

모든 멤버 판단의 비공개 명세와 공개 대응 JSON을 구분합니다.

보존 목록→원차수 설명/코드→도구 입력의 추적 순서를 유지합니다. 검증·정리·복구·배포 도구는 원경로와 기존 증거를 요구할 수 있어 일괄 실행이 안전한 인터페이스가 아닙니다. 실행 불가를 단일 제품 결함으로 판정하지 않고 시점·경로·해시·전체 차이를 조사합니다.

### LEVEL 4 · 원문이 연결한 파일과 구성

아래 경로는 이 설명의 추적 가능한 자료 경계입니다. 소스/설정/검증/파일 식별/라이선스를 구별하여 변경 영향과 재현 입력을 대조합니다. 파일 보존과 실행 검증을 별도 증거로 취급하며 이름만으로 구현의 완전성이나 배포 상태를 추론하지 않습니다.

| 경로 | 자료 종류 |
|---|---|
| [11차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/11%EC%B0%A8) | 하위 자료 폴더 |
| [12차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/12%EC%B0%A8) | 하위 자료 폴더 |
| [13차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/13%EC%B0%A8) | 하위 자료 폴더 |
| [14차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8) | 하위 자료 폴더 |
| [15차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/15%EC%B0%A8) | 하위 자료 폴더 |
| [16차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/16%EC%B0%A8) | 하위 자료 폴더 |
| [work/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/work) | 하위 자료 폴더 |

| 파일 | 원문에 정의된 역할·구성 |
|---|---|
| [14차/work/시험장치_복사.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/work/%EC%8B%9C%ED%97%98%EC%9E%A5%EC%B9%98_%EB%B3%B5%EC%82%AC.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/diagnose_long14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/diagnose_long14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/finish_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/finish_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/implement_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/implement_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/ownership_manifest14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/ownership_manifest14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair1_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair1_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair3_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair3_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair4_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair4_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/test_marker14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/test_marker14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |

### LEVEL 4 · 보존 범위와 전체 비교

전체 비교는 소스·설정·문서의 변경 추적 단위입니다. 추가/수정/삭제와 주석·공백·개행을 포함하고 바이너리는 크기/해시로 보존합니다. 행 증감은 의미상 변경과 일대일 대응하지 않으므로 기능 해석과 분리합니다. 확인된 이전 자료만 비교 기준으로 삼고 자료 공백을 추정으로 메우지 않습니다.

[전체 비교·자료 범위](REVISION_AUDIT.md)

이 묶음의 직전 제품 차수는 임의로 만들지 않습니다.

| 차수·위치 | 추가·변경 자료 수 |
|---|---:|
| 10차 | 11 |
| 11차 | 10 |
| 12차 | 10 |
| 13차 | 12 |
| 14차 | 10 |
| 15차 | 3 |
| 16차 | 8 |
| 1차 | 12 |
| 2차 | 4 |
| 3차 | 9 |
| 4차 | 12 |
| 5차 | 12 |
| 6차 | 10 |
| 7차 | 2 |
| 8차 | 10 |
| 9차 | 9 |
| 보존자료 | 170 |
| 실행점검 | 13 |
| 실행환경점검 | 6 |
| 코드 | 13 |

[파일 전체 명세](source-materials/files.json) · [공개 ZIP 멤버 대응](source-materials/archive-members.json)


<a id="reading-level-5"></a>
## LEVEL 5 - Expert

기존의 전체 설명·표·코드·함수 참조·파일별 비교는 아래에 그대로 이어집니다. [자료 폴더](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4) · [직전 비교·자료 범위](REVISION_AUDIT.md) · [이 차수의 원래 전체 상세](#reading-level-5)
<!-- reading-levels:end -->


[프로젝트](../README.md) · [전체 차수](REVISION_INDEX.md) · [원본 자료](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4)

<a name="-목적과-이-차수의-역할"></a>
## 목적과 이 차수의 역할

기존 모든 차수는 [첫 화면 차수표](../README.md)에서 탐색합니다. 이번에는 코드·제품 설정·사용법·설계·실험 기록·자체 제작 리소스·라이선스를 원본 경로와 함께 보강했습니다. 아래는 기존 공개 기준과 바이트가 다른 제품 자료 346개 목록입니다. 전체 차수의 공개 파일은 각 차수 비교 명세에 별도로 있습니다.

<a name="features"></a>

<a name="-핵심-기능과-보존-범위"></a>
## 핵심 기능과 보존 범위

**항목:** 1

**이 차수에서 확인할 내용:** 기존 모든 차수는 [첫 화면 차수표](../README.md)에서 탐색합니다.

이번에는 코드·제품 설정·사용법·설계·실험 기록·자체 제작 리소스·라이선스를 원본 경로와 함께 보강했습니다.

아래는 기존 공개 기준과 바이트가 다른 제품 자료 346개 목록입니다.

전체 차수의 공개 파일은 각 차수 비교 명세에 별도로 있습니다.

**항목:** 2

**이 차수에서 확인할 내용:** ZIP은 중앙 목록과 경로 탈출·링크·암호화·크기 경계를 검사하고 소스 멤버를 실제 읽었습니다.

기존 공개 파일과 바이트 또는 BOM·개행만 같은 멤버는 대응 링크로 남기고, 고유 소스는 `압축원본소스`에 보존했습니다.

실행 바이너리·캐시·실제 인증 자료·실제 운영 DB·실 거래 계좌 자료·원작 추출 표현 자산은 제외했습니다.

각 ZIP의 모든 멤버 판단은 비공개 검토 명세에, 공개 대응 멤버는 아래 JSON에 있습니다.


<a name="-입력처리출력"></a>
## 입력·처리·출력

| 단계 | 내용 |
| --- | --- |
| 입력 | 기존 차수에서 개별 보존한 작업 소스와 문서 |
| 처리 | 원래 경로·출처·파일 대응을 찾아 읽기 |
| 출력 | 기능 소스·개발 도구·설명 문서의 보존 목록 |

<a name="usage"></a>

<a name="️-화면과-사용-흐름"></a>
## 화면과 사용 흐름

1. 보존 목록에서 원래 차수·경로를 찾습니다.
2. 연결된 코드와 해당 차수의 설명을 함께 읽습니다.
3. 보조 도구와 제품 실행 코드를 구분하고 필요한 입력을 확인합니다.

<a name="structure"></a>

<a name="-실제-자료-구성"></a>
## 실제 자료 구성

| 경로 | 자료 종류 |
| --- | --- |
| [11차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/11%EC%B0%A8) | 하위 자료 폴더 |
| [12차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/12%EC%B0%A8) | 하위 자료 폴더 |
| [13차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/13%EC%B0%A8) | 하위 자료 폴더 |
| [14차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8) | 하위 자료 폴더 |
| [15차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/15%EC%B0%A8) | 하위 자료 폴더 |
| [16차/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/16%EC%B0%A8) | 하위 자료 폴더 |
| [work/](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/work) | 하위 자료 폴더 |

<a name="-주요-모듈과-코드-읽기"></a>
## 주요 모듈과 코드 읽기

| 파일 | 원문에 정의된 역할·구성 |
| --- | --- |
| [14차/work/시험장치_복사.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/work/%EC%8B%9C%ED%97%98%EC%9E%A5%EC%B9%98_%EB%B3%B5%EC%82%AC.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/diagnose_long14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/diagnose_long14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/finish_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/finish_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/implement_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/implement_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/ownership_manifest14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/ownership_manifest14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair1_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair1_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair3_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair3_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/repair4_ui14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/repair4_ui14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |
| [14차/source/work/test_marker14.py](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4/14%EC%B0%A8/source/work/test_marker14.py) | 소스 본문과 설정을 함께 읽는 구성요소 |

<a name="setup"></a>

<a name="-설치-환경과-실행사용법"></a>
## 설치 환경과 실행·사용법

이 묶음은 서로 다른 시점의 작업 자료를 원래 경로로 보관합니다. 한 앱의 완전한 같은 시점 설치 환경으로 합치지 않습니다.

아래 원본 목록에서 필요한 파일을 고른 뒤 해당 차수의 실행 안내와 대조합니다. 검증·캐시 정리·복구·배포 도구에는 원래 경로와 기존 증거가 필요할 수 있으므로 일괄 실행하지 않습니다.

<a name="results"></a>

<a name="-처리-결과와-출력-읽기"></a>
## 처리 결과와 출력 읽기

원래 위치와 파일 크기·SHA-256으로 보존 대응을 확인합니다. 같은 이름의 파일도 시점과 경로가 다르면 다른 자료입니다.

<a name="️-제약과-문제-해결"></a>
## 제약과 문제 해결

| 상황 | 확인할 내용 |
| --- | --- |
| 한 폴더로 실행되지 않음 | 이 묶음은 여러 작업 시점의 보존 자료이며 단일 배포 제품이 아닙니다. |
| 이전 파일과 내용이 다름 | 원래 차수·경로·해시와 전체 비교를 함께 확인합니다. |

<a name="changes"></a>

<a name="-직전-자료와-전체-비교"></a>
## 직전 자료와 전체 비교

이 자료의 보존 범위와 기준 파일을 연결합니다.

[전체 비교·자료 범위](REVISION_AUDIT.md)

추가·수정·삭제, 주석·공백·개행과 바이너리 크기·해시는 연결된 원래 비교 자료에서 확인합니다.

<details>
<summary>파일별 비교·크기·해시 자세히 보기</summary>

| 차수·위치 | 추가·변경 자료 수 |
| --- | --- |
| 10차 | 11 |
| 11차 | 10 |
| 12차 | 10 |
| 13차 | 12 |
| 14차 | 10 |
| 15차 | 3 |
| 16차 | 8 |
| 1차 | 12 |
| 2차 | 4 |
| 3차 | 9 |
| 4차 | 12 |
| 5차 | 12 |
| 6차 | 10 |
| 7차 | 2 |
| 8차 | 10 |
| 9차 | 9 |
| 보존자료 | 170 |
| 실행점검 | 13 |
| 실행환경점검 | 6 |
| 코드 | 13 |

[파일 전체 명세](source-materials/files.json) · [공개 ZIP 멤버 대응](source-materials/archive-members.json)

</details>

[프로젝트](../README.md) · [전체 차수](REVISION_INDEX.md) · [원본 자료](../%EB%B3%B4%EC%A1%B4%EC%9E%90%EB%A3%8C/%EC%9E%91%EC%97%85%EC%86%8C%EC%8A%A4)
