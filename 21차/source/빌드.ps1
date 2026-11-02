$ErrorActionPreference = 'Stop'
$project21 = 'D:\2026\PXE Project'
$source21 = Join-Path $project21 '21차\source'
$build21 = Join-Path $project21 'work\build21\source'
$delivery21 = Join-Path $project21 '21차'
$python21 = 'C:\Users\PUVKID\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
& $python21 -B -X utf8 (Join-Path $source21 'tools\sync_build.py')
if ($LASTEXITCODE -ne 0) { throw '기준 소스와 실제 빌드 사본 대조 실패' }
$env:JAVA_HOME = Join-Path $project21 'work\tools\jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $project21 '코드\NEXTools\android-sdk'
$env:NEX_SIGNING_DIR = (Join-Path $project21 '코드\NEXSigning').Replace('\','/')
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'macro.jks'))) { throw '기존 앱과 같은 개인 서명 키가 필요합니다.' }
Push-Location $build21
try {
    $gradle21 = Join-Path $project21 'work\tools\gradle-8.13\bin\gradle.bat'
    & $gradle21 :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain --no-watch-fs --no-daemon --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw '휴대전화 APK 빌드 또는 검사 실패' }
    Copy-Item -LiteralPath (Join-Path $build21 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery21 'PXE_21차_v2.1_노트10플러스.apk')
    & $gradle21 :app:assembleRelease :app:assembleReleaseAndroidTest -PNEX_PC --console=plain --no-watch-fs --no-daemon --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw 'PC APK 또는 기기 검사 APK 빌드 실패' }
    Copy-Item -LiteralPath (Join-Path $build21 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery21 'PXE_21차_v2.1_PC용.apk')
} finally { Pop-Location }
Write-Host '완료: 21차 폴더에 휴대전화용과 PC용 APK를 저장했습니다.'
