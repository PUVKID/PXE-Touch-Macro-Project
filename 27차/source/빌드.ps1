param([string]$Python='python')
$ErrorActionPreference='Stop'
& $Python -B -X utf8 (Join-Path $PSScriptRoot 'tools/build_release.py')
if ($LASTEXITCODE -ne 0) { throw '빌드 또는 검사 실패' }
