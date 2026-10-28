# PXE 15차 · 1.5.0

Android 접근성 서비스와 ML Kit 한국어 OCR로 글자 조건·터치·제스처 매크로를 실행합니다. 앱 ID와 저장 형식은 유지하며 기존 설치 앱과 같은 개인 서명으로 만든 APK만 덮어 업데이트할 수 있습니다.

## 변경 범위

- 목록 탐지와 개별 글자 동작의 인식 방식을 기본 설정에서 고르고 적용 범위를 확인합니다. 글자 탭의 선택은 그 탭의 단독 실행 및 글자 탭에서 추가하는 동작에만 적용됩니다.
- 실제 오류 단계와 요청/실제 인식 방식을 구분합니다. 화면 정보는 OCR을 호출하지 않으며, 이미지 글자를 제공하지 않는 앱에서 접근성 텍스트를 만들어 내지는 않습니다.
- 캡처 간격은 API30에서 1.1초, API31 이상에서 0.5초를 기준으로 합니다. 간격 거절 시 최대12초까지 후퇴하고 실제 성공 뒤 점진적으로 기준에 복귀합니다. 이는 기기별 성능 보장이나 숨은 OS API 호출이 아닙니다.
- 목록 확인의 추가2초 대기를 줄이고, 세부 설정의 인식 즉시 들어가기가 켜진 경우에만 터치 전 재확인을 생략합니다. 기본은 재확인을 유지합니다.
- 기본 설정과 동작 사이 대기·세부 설정을 나누어 접습니다. 기존 값·1~6순위·동일 글자 순번·중복 방지·계속 기다리기·중단 기능은 유지합니다.
- OCR은 전체 이미지와 기존 중심점 영역 필터를 유지합니다. 경계 글자 손실 위험이 있어 이번 차수에는 영역 자르기를 도입하지 않았습니다.

## 개발 환경

Java17, Gradle8.13, Android Gradle Plugin8.9.1, compile/target SDK35, min SDK30, ML Kit 한국어16.0.1입니다. 나눔고딕 Bold 파일과 기존 OFL 고지를 포함합니다. 프로젝트 전체의 새 라이선스는 부여하지 않습니다.

일반 개발 환경에서는 Android SDK를 local.properties 또는 ANDROID_HOME으로 지정한 후 Gradle wrapper를 사용할 수 있습니다.

```powershell
.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleRelease :app:assembleReleaseAndroidTest -PNEX_PC
```

개인 키와 key.properties는 공개하지 않습니다. NEX_SIGNING_DIR가 유효한 개인 서명 폴더를 가리키지 않으면 개발 구성은 debug 서명을 선택하므로 기존 설치본 업데이트용으로 사용하지 않습니다. 납품 빌드.ps1은 프로젝트 소유의 D:/2026/PXE15Build 전용 연결과 기존 개인 키 존재를 확인합니다. PC는 ARM64/x86_64, 휴대전화는 ARM64를 포함합니다.

## 검사 범위

필수 검사 명세는 tools/required-check-plan.json, 추가 인식/UI 검사는 tools/recognition-check-plan.json입니다. 기기 검사 도구는 소유 PXE15 AVD, 별도 ADB5043, emulator5598, 설치 버전15를 확인한 후 실행합니다. 사용자 블루스택을 자동 선택하지 않습니다. 서명/원본 보존 검사는 별도로 보관한 이전 차수 APK와 로컬 보존 기준이 필요합니다.

실제 사용자 블루스택의 처리 속도, 5개 동시 실행, 24시간 무중단은 이 시험 구성의 검증 범위가 아닙니다. OS가 캡처를 제한하는 화면은 우회하지 않고 해당 단계에서 원인을 표시합니다.

## 공식 근거

- [Android31 AccessibilityService 원본](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/android-31/+/refs/heads/androidx-savedstate-release/android/accessibilityservice/AccessibilityService.java)
- [Android30 AccessibilityService 원본](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/android-30/+/refs/heads/main/android/accessibilityservice/AccessibilityService.java)
- [ML Kit 입력 이미지와 성능 안내](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
