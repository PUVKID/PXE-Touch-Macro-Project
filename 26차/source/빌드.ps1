param([string]$Python='python')
$ErrorActionPreference='Stop'
$sourceRoot=$PSScriptRoot
$projectRoot=Split-Path (Split-Path $sourceRoot -Parent) -Parent
$buildRoot=Join-Path $projectRoot 'work/build26/source'
$env:JAVA_HOME=Join-Path $projectRoot 'work/tools/jdk-17.0.20.1+1'
$env:ANDROID_HOME=Join-Path $projectRoot '코드/NEXTools/android-sdk'
$env:NEX_SIGNING_DIR=Join-Path $projectRoot '코드/NEXSigning'
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'key.properties'))) { throw '기존 개인 서명 키가 필요합니다. 소스에는 키를 넣지 않습니다.' }
& $Python -B -X utf8 (Join-Path $sourceRoot 'tools/sync_build.py')
if ($LASTEXITCODE -ne 0) { throw '빌드 사본 대조 실패' }
Push-Location $buildRoot
try {
 & (Join-Path $projectRoot 'work/tools/gradle-8.13/bin/gradle.bat') :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:assembleReleaseAndroidTest -PNEX_PC --console=plain --no-watch-fs --no-daemon --max-workers=1 --project-cache-dir work/.gradle '-Dorg.gradle.jvmargs=-Xmx1536m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'
 if ($LASTEXITCODE -ne 0) { throw '빌드 또는 검사 실패' }
 Copy-Item -LiteralPath 'work/build/app/outputs/apk/release/app-release.apk' -Destination (Join-Path $projectRoot '26차/PXE_26차_v2.6_공용.apk')
} finally { Pop-Location }
