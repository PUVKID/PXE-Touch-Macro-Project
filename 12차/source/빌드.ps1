$ErrorActionPreference = 'Stop'
$macroRoot12 = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$bridgeTarget12 = Join-Path $macroRoot12 'work\build12'
$bridge12 = Join-Path $env:TEMP 'NEX_Build_12'
if (!(Test-Path -LiteralPath $bridgeTarget12)) { throw '프로젝트 work\build12 연결 폴더가 없습니다.' }
if (Test-Path -LiteralPath $bridge12) {
    $existing12 = Get-Item -LiteralPath $bridge12
    if ($existing12.LinkType -ne 'Junction' -or $existing12.Target -ne $bridgeTarget12) { throw '임시 빌드 경로가 다른 항목에서 사용 중입니다.' }
} else { New-Item -ItemType Junction -Path $bridge12 -Target $bridgeTarget12 | Out-Null }
$env:JAVA_HOME = Join-Path $bridge12 'tools\jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $bridge12 'android\android-sdk'
$env:NEX_SIGNING_DIR = (Join-Path $bridge12 'signing').Replace('\','/')
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'macro.jks'))) { throw '기존 앱과 같은 개인 서명 키가 필요합니다.' }
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'local.properties'), 'sdk.dir='+$env:ANDROID_HOME.Replace('\','/'), [Text.UTF8Encoding]::new($false))
$delivery12 = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $bridge12 'source')
try {
    $gradle12 = Join-Path $bridge12 'tools\gradle-8.13\bin\gradle.bat'
    & $gradle12 :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain --no-watch-fs --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw '휴대전화 APK 빌드 또는 검사 실패' }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery12 'PXE_12차_v1.2_노트10플러스.apk')
    & $gradle12 :app:assembleRelease :app:assembleReleaseAndroidTest -PNEX_PC --console=plain --no-watch-fs --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw 'PC APK 또는 기기 검사 APK 빌드 실패' }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery12 'PXE_12차_v1.2_PC용.apk')
} finally { Pop-Location }
Write-Host '완료: 12차 폴더에 휴대전화용과 PC용 APK를 저장했습니다.'
