# PXE 28차 소스 · Astryx 네이티브 화면

기존 매크로·내장 오프라인 한글 OCR·최대 6개 우선 영역·반복·분기·중복 방지·일시정지/정지를 유지합니다. 16차 기준 네 파일을 유지하면서 모든 탭 실행 제어·글자/탐지 재시작·작은 진행 위젯과 PC 빌드 시 Rust 색상 생성 경로를 정리했습니다.

| 자료 | 내용 |
|---|---|
| [사용 안내](../사용안내.md) | 기기 설치·설정·실행·제어 |
| [검증 결과](../검증_결과.md) | 통과한 검사·실제 확인 범위와 남은 절차 |
| [빌드 방법](빌드방법.txt) | 실제 환경 변수와 빌드 단계 |
| [28차 설계](docs/28차_Astryx_계획.md) | 변경 경계와 확인 조건 |

[LEVEL 1](../상세설명.md#user-content-reading-level-1) · [LEVEL 2](../상세설명.md#user-content-reading-level-2) · [LEVEL 3](../상세설명.md#user-content-reading-level-3) · [LEVEL 4](../상세설명.md#user-content-reading-level-4) · [LEVEL 5](../상세설명.md#user-content-reading-level-5)

Rust 테마 생성기는 `theme-engine`에 있고 `tools/generate_astryx.py`가 기존 두 팔레트를 대조해 Android 12색 리소스를 생성합니다. Rust를 Android 매크로·OCR 런타임으로 바꾼 것은 아닙니다. 제품·단위·린트·기기 검사 결과는 확정 후 결과 문서에 반영합니다.

| 구현 | 현재 범위 |
|---|---|
| 기본 정의 | WatchConfig·Step·Macro·CapturePacer는 16차·26차·현재28 바이트 동일 |
| 실행 제어 | MainActivity의 네 탭·MacroService 검증 후 재시작·Floating 글자 편집 취소 복원 |
| 작은 HUD | 104×44dp 기본·280dp 상세·숫자 드래그·별도 불투명도 |
| 화면·모델 | 3D 장식 제거, 학습형 행동 AI 없음, MLKit 한글 OCR 유지 |
| 검증 상태 | Rust 6개 검사 통과, 단위 102개 통과·최종 린트 오류0/경고0·Android12 설치 성공, 기기 11개 방법 통과, 독립 종합·패키지·공개 상태는 별도 차수 검증 문서에서 확인 |

공개 소스에는 theme-engine/astryx-theme.exe를 포함하지 않습니다. 먼저 theme-engine의 Rust 빌드를 실행하고 target/release/astryx-theme.exe를 해당 위치로 복사한 뒤 Android 빌드를 진행하세요. 실제 실행 환경과 명령은 [빌드 방법](빌드방법.txt)에 설명했습니다. 앱을 사용하는 Android 기기에 Rust를 설치할 필요는 없습니다.
