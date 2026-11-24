# 앱의 저장·연동·검사에 쓰는 내부 이름은 유지하고 외부 미사용 코드를 축소합니다.
-keep,includedescriptorclasses class kr.yunseok.touchmacro.** { *; }
# 접근성·검사 도구와 공유하는 AndroidX 및 Kotlin 클래스의 이름·연동을 보존합니다.
-keep class androidx.** { *; }
-keep class kotlin.** { *; }
# OCR 인식기 인터페이스와 결과 구조를 외부 연결·검사에서도 그대로 사용합니다.
-keep class com.google.mlkit.vision.text.** { *; }
# OCR 검사에서 사용하는 이미지 정보와 비동기 결과의 공개 메서드를 보존합니다.
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
