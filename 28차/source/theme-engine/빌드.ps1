param([ValidateSet('test','release')][string]$Mode='release')
$ErrorActionPreference='Stop'
$toolRoot='E:/2026/개발 도구 및 캐시/Rust Astryx'
& 'C:/Program Files/Microsoft Visual Studio/2022/Community/Common7/Tools/Launch-VsDevShell.ps1' -Arch amd64 -HostArch amd64 -SkipAutomaticLocation | Out-Null
$env:RUSTUP_HOME=$toolRoot+'/rustup'
$env:CARGO_HOME=$toolRoot+'/cargo'
$env:PATH=$toolRoot+'/cargo/bin;'+$env:PATH
$env:LIB=$toolRoot+'/windows-sdk/x64/c/um/x64;'+$toolRoot+'/windows-sdk/x64/c/ucrt/x64;'+$env:LIB
Push-Location -LiteralPath $PSScriptRoot
try {
    if($Mode -eq 'test'){ & ($toolRoot+'/cargo/bin/cargo.exe') test }
    else { & ($toolRoot+'/cargo/bin/cargo.exe') build --release }
    if($LASTEXITCODE -ne 0){throw "Rust 빌드 실패: $LASTEXITCODE"}
} finally { Pop-Location }
