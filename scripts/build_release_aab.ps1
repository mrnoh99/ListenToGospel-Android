# Builds a signed release App Bundle (.aab) for Google Play.
# Requires: keystore.properties + upload-keystore.jks, AudioFiles in assets.

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

if (-not (Test-Path "keystore.properties")) {
    Write-Host "Missing keystore.properties. Run first:"
    Write-Host "  scripts\create_release_keystore.ps1"
    exit 1
}

$audioDir = "app\src\main\assets\AudioFiles"
if (-not (Test-Path $audioDir)) {
    Write-Host "Missing $audioDir — copy audio before release build."
    exit 1
}
$m4aCount = (Get-ChildItem $audioDir -Recurse -Filter *.m4a -File).Count
if ($m4aCount -lt 89) {
    Write-Host "Warning: expected 89 .m4a files, found $m4aCount"
}

$jbr = "${env:ProgramFiles}\Android\Android Studio\jbr"
if (Test-Path $jbr) {
    $env:JAVA_HOME = $jbr
    $env:PATH = "$jbr\bin;$env:PATH"
}

Write-Host "Building signed release bundle..."
& .\gradlew.bat bundleRelease --no-daemon

$bundle = Get-ChildItem -Path "app\build\outputs\bundle\release" -Filter "*.aab" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($bundle) {
    Write-Host ""
    Write-Host "SUCCESS: $($bundle.FullName)"
    Write-Host "Size MB: $([math]::Round($bundle.Length / 1MB, 2))"
    Write-Host "Upload this file to Play Console -> App bundle"
} else {
    Write-Host "Build finished but .aab not found under app\build\outputs\bundle\release"
    exit 1
}
