$ErrorActionPreference = 'Stop'
$source13 = 'D:\2026\PXE Project\13차\source'
$bridge13 = 'D:\PXE13Build'
$delivery13 = 'D:\2026\PXE Project\13차'
$sourceLink13 = Get-Item -LiteralPath (Join-Path $bridge13 'source')
if ($sourceLink13.LinkType -ne 'Junction' -or $sourceLink13.Target -ne $source13) { throw '13차 전용 빌드 연결을 확인하세요.' }
$env:JAVA_HOME = Join-Path $bridge13 'tools\jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $bridge13 'android\android-sdk'
$env:NEX_SIGNING_DIR = (Join-Path $bridge13 'signing').Replace('\','/')
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'macro.jks'))) { throw '기존 앱과 같은 개인 서명 키가 필요합니다.' }
[IO.File]::WriteAllText((Join-Path $source13 'local.properties'), 'sdk.dir='+$env:ANDROID_HOME.Replace('\','/'), [Text.UTF8Encoding]::new($false))
Push-Location (Join-Path $bridge13 'source')
try {
    $gradle13 = Join-Path $bridge13 'tools\gradle-8.13\bin\gradle.bat'
    & $gradle13 :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain --no-watch-fs --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw '휴대전화 APK 빌드 또는 검사 실패' }
    Copy-Item -LiteralPath (Join-Path $source13 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery13 'PXE_13차_v1.3_노트10플러스.apk')
    & $gradle13 :app:assembleRelease :app:assembleReleaseAndroidTest -PNEX_PC --console=plain --no-watch-fs --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw 'PC APK 또는 기기 검사 APK 빌드 실패' }
    Copy-Item -LiteralPath (Join-Path $source13 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery13 'PXE_13차_v1.3_PC용.apk')
} finally { Pop-Location }
Write-Host '완료: 13차 폴더에 휴대전화용과 PC용 APK를 저장했습니다.'
