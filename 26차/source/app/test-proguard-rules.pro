# AndroidX 검사 도구의 컴파일용 주석 세 종류에만 적용합니다.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.MustBeClosed
-dontwarn kotlin.jvm.internal.SourceDebugExtension
# 실제 클래스·메서드 이름으로 선택 실행하는 기기 검사를 보존합니다.
-keep class kr.yunseok.touchmacro.** { *; }
-keep class androidx.test.** { *; }
