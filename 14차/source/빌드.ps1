$ErrorActionPreference = 'Stop'
$source14 = 'D:\2026\PXE Project\14차\source'
$bridge14 = 'D:\PXE14Build'
$delivery14 = 'D:\2026\PXE Project\14차'
$sourceLink14 = Get-Item -LiteralPath (Join-Path $bridge14 'source')
if ($sourceLink14.LinkType -ne 'Junction' -or $sourceLink14.Target -ne $source14) { throw '14차 전용 빌드 연결을 확인하세요.' }
$env:JAVA_HOME = Join-Path $bridge14 'tools\jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $bridge14 'android\android-sdk'
$env:NEX_SIGNING_DIR = (Join-Path $bridge14 'signing').Replace('\','/')
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'macro.jks'))) { throw '기존 앱과 같은 개인 서명 키가 필요합니다.' }
[IO.File]::WriteAllText((Join-Path $source14 'local.properties'), 'sdk.dir='+$env:ANDROID_HOME.Replace('\','/'), [Text.UTF8Encoding]::new($false))
Push-Location (Join-Path $bridge14 'source')
try {
    $gradle14 = Join-Path $bridge14 'tools\gradle-8.13\bin\gradle.bat'
    & $gradle14 :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain --no-watch-fs --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw '휴대전화 APK 빌드 또는 검사 실패' }
    Copy-Item -LiteralPath (Join-Path $source14 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery14 'PXE_14차_v1.4_노트10플러스.apk')
    & $gradle14 :app:assembleRelease :app:assembleReleaseAndroidTest -PNEX_PC --console=plain --no-watch-fs --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw 'PC APK 또는 기기 검사 APK 빌드 실패' }
    Copy-Item -LiteralPath (Join-Path $source14 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery14 'PXE_14차_v1.4_PC용.apk')
} finally { Pop-Location }
Write-Host '완료: 14차 폴더에 휴대전화용과 PC용 APK를 저장했습니다.'
