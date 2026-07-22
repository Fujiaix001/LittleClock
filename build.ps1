$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$dist = Join-Path $projectRoot 'dist'
$rootDir = Split-Path -Parent $projectRoot
$rootDist = Join-Path $rootDir 'dist'

Write-Host "Building LittleClock v1.3.2 Release APK..." -ForegroundColor Cyan

Push-Location $projectRoot
try {
    & .\gradlew.bat :app:lintRelease :app:assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
} finally {
    Pop-Location
}

New-Item -ItemType Directory -Path $dist -Force | Out-Null
New-Item -ItemType Directory -Path $rootDist -Force | Out-Null

$apkSource = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
$apkDist = Join-Path $dist 'LittleClock-v1.3.2.apk'
$apkRootDist = Join-Path $rootDist 'LittleClock-v1.3.2.apk'

Copy-Item -LiteralPath $apkSource -Destination $apkDist -Force
Copy-Item -LiteralPath $apkSource -Destination $apkRootDist -Force

$hash = (Get-FileHash -LiteralPath $apkDist -Algorithm SHA256).Hash
Set-Content -LiteralPath (Join-Path $dist 'SHA256SUMS.txt') -Value "$hash  LittleClock-v1.3.2.apk" -Encoding ascii

Write-Host "Successfully built LittleClock-v1.3.2.apk" -ForegroundColor Green
Get-ChildItem -LiteralPath $dist -File | Select-Object Name, Length, LastWriteTime
