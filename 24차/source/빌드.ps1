$ErrorActionPreference = 'Stop'
function Initialize-PxeBuildDirectory([string] $Path) {
    # 기존 상위 경로부터 일반 폴더인지 검사하고 없는 폴더만 실제로 준비합니다.
    $pending = [System.Collections.Generic.List[System.IO.DirectoryInfo]]::new()
    $current = [System.IO.DirectoryInfo]::new([System.IO.Path]::GetFullPath($Path))
    while ($null -ne $current) {
        $pxeLookupErrors = @()
        $item = Get-Item -LiteralPath $current.FullName -Force -ErrorAction SilentlyContinue -ErrorVariable pxeLookupErrors
        if ($null -ne $item) {
            if (!$item.PSIsContainer -or ($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
                throw '빌드 경로에는 일반 폴더만 사용할 수 있습니다.'
            }
        } else {
            if (@($pxeLookupErrors | Where-Object { $_.CategoryInfo.Category -ne [System.Management.Automation.ErrorCategory]::ObjectNotFound }).Count -gt 0) {
                throw '빌드 조상 경로를 확인할 수 없습니다.'
            }
            $pending.Add($current)
        }
        $current = $current.Parent
    }
    for ($index = $pending.Count - 1; $index -ge 0; $index--) {
        [System.IO.Directory]::CreateDirectory($pending[$index].FullName) | Out-Null
        $item = Get-Item -LiteralPath $pending[$index].FullName -Force
        if (!$item.PSIsContainer -or ($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
            throw '빌드 작업 폴더를 안전하게 준비하지 못했습니다.'
        }
    }
}

$project24 = if ($env:PXE_PROJECT_ROOT) { $env:PXE_PROJECT_ROOT } else { Split-Path (Split-Path $PSScriptRoot -Parent) -Parent }
$source24 = Join-Path $project24 '24차\source'
$build24 = if ($env:PXE_BUILD_ROOT) { $env:PXE_BUILD_ROOT } else { Join-Path $project24 'work\build24\source' }
$delivery24 = Join-Path $project24 '24차'
$python24 = $env:PXE_PYTHON
if (!$python24 -or !(Test-Path -LiteralPath $python24 -PathType Leaf)) { throw 'PXE_PYTHON에 실제 Python 실행 파일을 지정하세요.' }
Initialize-PxeBuildDirectory $build24
& $python24 -B -X utf8 (Join-Path $source24 'tools\sync_build.py')
if ($LASTEXITCODE -ne 0) { throw '기준 소스와 실제 빌드 사본 대조 실패' }
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath $env:JAVA_HOME -PathType Container)) { throw 'JAVA_HOME에 JDK17 폴더를 지정하세요.' }
if (!$env:ANDROID_HOME -or !(Test-Path -LiteralPath $env:ANDROID_HOME -PathType Container)) { throw 'ANDROID_HOME에 Android SDK35 폴더를 지정하세요.' }
if (!$env:NEX_SIGNING_DIR) { throw 'NEX_SIGNING_DIR에 공개 저장소 밖의 기존 서명 폴더를 지정하세요.' }
if (!(Test-Path -LiteralPath (Join-Path $env:NEX_SIGNING_DIR 'macro.jks'))) { throw '기존 앱과 같은 개인 서명 키가 필요합니다.' }
Push-Location $build24
try {
    $gradle24 = $env:PXE_GRADLE
    if (!$gradle24 -or !(Test-Path -LiteralPath $gradle24 -PathType Leaf)) { throw 'PXE_GRADLE에 실제 Gradle 실행 파일을 지정하세요.' }
    & $gradle24 :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:assembleReleaseAndroidTest -PNEX_PC --console=plain --no-watch-fs --no-daemon --project-cache-dir work/.gradle
    if ($LASTEXITCODE -ne 0) { throw '공용 APK 빌드 또는 검사 실패' }
    Copy-Item -LiteralPath (Join-Path $build24 'work\build\app\outputs\apk\release\app-release.apk') -Destination (Join-Path $delivery24 'PXE_24차_v2.4_공용.apk')
} finally { Pop-Location }
Write-Host '완료: 24차 폴더에 공용 APK 하나를 저장했습니다.'
