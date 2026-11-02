$ErrorActionPreference = 'Stop'
$project = 'D:/2026/PXE Project'
$buildRoot = "$project/work/build20/source"
$env:JAVA_HOME = "$project/work/tools/jdk-17.0.20.1+1"
$env:ANDROID_HOME = "$project/코드/NEXTools/android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:NEX_SIGNING_DIR = "$project/코드/NEXSigning"
$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
# 기존 키는 Gradle 내부에서만 사용하며 제품 납품본은 복사·덮어쓰지 않습니다.
Push-Location -LiteralPath $buildRoot
try {
    & "$project/work/tools/gradle-8.13/bin/gradle.bat" ':app:assembleReleaseAndroidTest' '-PNEX_PC' '--console=plain' '--no-watch-fs' '--no-daemon' '--project-cache-dir' 'work/.gradle'
    if ($LASTEXITCODE -ne 0) { throw "시험 APK 빌드 실패: $LASTEXITCODE" }
} finally { Pop-Location }
