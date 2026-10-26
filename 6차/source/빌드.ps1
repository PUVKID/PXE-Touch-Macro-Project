$ErrorActionPreference = 'Stop'
$projectRoot4 = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$bridgeTarget4 = Join-Path $projectRoot4 'work\build6'
$bridge4 = Join-Path $env:TEMP 'NEX_Build_6'
if (!(Test-Path -LiteralPath $bridgeTarget4)) { throw '프로젝트 work\build6 연결 폴더가 없습니다.' }
if (Test-Path -LiteralPath $bridge4) {
    $existing4 = Get-Item -LiteralPath $bridge4
    if ($existing4.LinkType -ne 'Junction' -or $existing4.Target -ne $bridgeTarget4) { throw '임시 빌드 경로가 다른 항목에서 사용 중입니다.' }
} else { New-Item -ItemType Junction -Path $bridge4 -Target $bridgeTarget4 | Out-Null }
$env:JAVA_HOME = Join-Path $bridge4 'tools\jdk-17.0.20.1+1'
$env:ANDROID_HOME = Join-Path $bridge4 'android\android-sdk'
$env:NEX_SIGNING_DIR = (Join-Path $bridge4 'signing').Replace('\','/')
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'macro.jks'))) { throw '기존 앱과 같은 개인 서명 키가 필요합니다.' }
$sdk4 = $env:ANDROID_HOME.Replace('\','/')
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'local.properties'), 'sdk.dir='+$sdk4, [Text.UTF8Encoding]::new($false))
Push-Location (Join-Path $bridge4 'source')
try {
    & (Join-Path $bridge4 'tools\gradle-8.13\bin\gradle.bat') :app:assembleRelease :app:testDebugUnitTest :app:lintDebug --console=plain --no-watch-fs
    if ($LASTEXITCODE -ne 0) { throw '빌드 또는 검사 실패' }
} finally { Pop-Location }
Write-Host '완료: source\app\build\outputs\apk\release\app-release.apk'
