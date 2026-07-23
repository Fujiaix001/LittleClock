$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$dist = Join-Path $projectRoot 'dist'
$buildConfig = Join-Path $projectRoot 'app\build.gradle.kts'
$variants = @(
    @{
        Gradle = 'StoropiaTestRelease'
        Source = 'storopiaTest\release\app-storopiaTest-release.apk'
        Name = 'LittleClock-v2.2.0-test-storopia.apk'
    },
    @{
        Gradle = 'StandardRelease'
        Source = 'standard\release\app-standard-release.apk'
        Name = 'LittleClock-v2.2.1.apk'
    }
)

Write-Host 'Building LittleClock paired APKs...' -ForegroundColor Cyan

Push-Location $projectRoot
try {
    & .\gradlew.bat :app:test :app:lintStoropiaTestRelease :app:lintStandardRelease `
        :app:assembleStoropiaTestRelease :app:assembleStandardRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
} finally {
    Pop-Location
}

New-Item -ItemType Directory -Path $dist -Force | Out-Null

$hashLines = foreach ($variant in $variants) {
    $apkSource = Join-Path $projectRoot "app\build\outputs\apk\$($variant.Source)"
    $apkDist = Join-Path $dist $variant.Name
    Copy-Item -LiteralPath $apkSource -Destination $apkDist -Force
    $hash = (Get-FileHash -LiteralPath $apkDist -Algorithm SHA256).Hash
    "$hash  $($variant.Name)"
}
Set-Content -LiteralPath (Join-Path $dist 'SHA256SUMS.txt') -Value $hashLines -Encoding ascii

Write-Host 'Successfully built both APKs.' -ForegroundColor Green
Get-ChildItem -LiteralPath $dist -File | Select-Object Name, Length, LastWriteTime
