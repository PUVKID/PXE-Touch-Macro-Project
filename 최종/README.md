# PXE 최종·24차

**현재 앱 버전은 2.4.0입니다.** 휴대 전화와 64비트 Android 에뮬레이터에서 같은 공용 APK를 사용합니다.

[사용 안내](사용안내.md) · [24차 기능 설명](../docs/versions/24차.md#reading-level-1) · [소스와 빌드](../24차/source/README.md) · [검증 결과](../24차/검증_결과.md)

## 파일

| 파일 | 크기 | 제공 위치 |
| --- | ---: | --- |
| PXE_24차 _v2.4 _공용.apk | 78,473,602 바이트 | 로컬 최종 폴더 |
| PXE_24차 _v2.4 _공용.zip | 31,271,319 바이트 | 로컬 최종 폴더·같은 APK 1개 포함 |
| 사용 안내.md | 5,818 바이트 | 로컬 최종 폴더·GitHub 사용 안내 |

APK SHA-256: `6b45cc8c7b3dc43bedc1f91811e3de63bd583e22ee3830a9fff2a2cee7f489c6`

ZIP SHA-256: `2f5d0652f960b9b7bdefdf280e426fc37d9302e4bb4f50af35e3535cc8776dcb`

**APK와 ZIP은 로컬 최종 폴더에 제공됩니다. GitHub에는 바이너리 다운로드가 등록돼 있지 않습니다.** 공개 저장소에서는 사용 안내·소스·파일 지문을 제공합니다.

<!-- reading-levels:start -->
## 읽는 수준 선택

[LEVEL 1 - Beginner](#reading-level-1) · [LEVEL 2 - Explorer](#reading-level-2) · [LEVEL 3 - Practitioner](#reading-level-3) · [LEVEL 4 - Advanced](#reading-level-4) · [LEVEL 5 - Expert](#reading-level-5)
<!-- reading-levels:end -->

<a id="reading-level-1"></a>
<a id="current-final-level-1"></a>

## LEVEL 1 - Beginner · 설치와 시작

APK는 Android에 설치하는 앱 파일이고 ZIP은 같은 APK 한 개를 담은 압축 파일입니다.

Android 11 이상, ARM64 또는 x86_64 환경에서 사용합니다.

업데이트 전에 개인 매크로와 시범 파일을 따로 보관하고, 설치 뒤 접근성·화면 위 표시 권한을 설정합니다.

기존 매크로가 기본이며 AI를 켜지 않아도 사용할 수 있습니다.

[24차 같은 수준 상세](../docs/versions/24차.md#reading-level-1) · [같은 수준 프로젝트](../docs/읽기안내/레벨1.md)

<a id="reading-level-2"></a>
<a id="current-final-level-2"></a>

## LEVEL 2 - Explorer · 동작 흐름

찾을 글자·영역·매크로를 입력하면 접근성 화면 정보 또는 한글 OCR로 조건을 확인합니다. 조건을 만족한 현재 위치를 터치하며, 미발견 시 뒤로 가기·스크롤·새로 고침 등의 분기를 설정합니다. 선택형 AI는 글자와 그림 시범으로 등록한 행동을 고르고 이미지 위치를 현재 화면에서 다시 찾습니다.

[24차 같은 수준 상세](../docs/versions/24차.md#reading-level-2) · [같은 수준 프로젝트](../docs/읽기안내/레벨2.md)

<a id="reading-level-3"></a>
<a id="current-final-level-3"></a>

## LEVEL 3 - Practitioner · 설정과 사용

글자 동작에서 계속 기다리기를 켜면 다음 글자가 나타날 때까지 대기합니다. 미발견 복구를 쓰려면 계속 기다리기를 끄고 최대 대기 시간과 실패 동작을 정합니다. 전체 반복과 개별 동작 반복은 별도입니다. Ⅱ/▶로 일시정지·계속 실행하고 ■로 중단합니다.

[24차 같은 수준 상세](../docs/versions/24차.md#reading-level-3) · [같은 수준 프로젝트](../docs/읽기안내/레벨3.md)

<a id="reading-level-4"></a>
<a id="current-final-level-4"></a>

## LEVEL 4 - Advanced · 호환성과 실행 경계

시각 정책은 1160차원 입력이며 구형 584차원 모델 읽기를 지원합니다. v2 packed에는 시범·행동 사전·가중치·RNG·학습 상태를 함께 저장합니다. 이미 보낸 입력의 응답이 늦으면 같은 future를 기다립니다. 캡처 준비 재시도와 터치 재전송을 구분하며 취소·일시 정지·대상 앱 경계를 유지합니다.

[24차 같은 수준 상세](../docs/versions/24차.md#reading-level-4) · [같은 수준 프로젝트](../docs/읽기안내/레벨4.md)

<a id="reading-level-5"></a>
<a id="current-final-level-5"></a>

## LEVEL 5 - Expert · 제품과 검증 근거

정상 빌드 13에서 단위 44 클래스·280개, 린 트 오류 0개·경고 4개를 확인했습니다. 전용 시험은 기능 1개와 시 각 2개입니다. 기기 시험 제품과 최종 APK의 내부 payload는 동일하고 APK 전체는 서명 블록 차이로 다릅니다. 실제 게임 속도·다른 설치 이식·장기 무중단 운용은 측정하지 않았습니다.

[24차 같은 수준 상세](../docs/versions/24차.md#reading-level-5) · [같은 수준 프로젝트](../docs/읽기안내/레벨5.md)

## 바뀐 점·차이점

이전 납품본과의 파일 크기·지문·구성 차이는 [최종 파일 교체 비교](../docs/revision-audit/최종.md)에 모았습니다.
