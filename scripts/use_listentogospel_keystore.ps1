# Configures keystore.properties for the iCloud release keystore and builds signed AAB.
#
#   scripts\use_listentogospel_keystore.ps1
#   scripts\use_listentogospel_keystore.ps1 -StorePassword "..." -KeyPassword "..." -Alias "upload"
#   scripts\use_listentogospel_keystore.ps1 -ConfigureOnly

param(
    [string]$KeystorePath = "C:\Users\jsnoh\iCloudDrive\AppDevelop\KeyStoreFile\listentogospel-release.jks",
    [string]$StorePassword,
    [string]$KeyPassword,
    [string]$Alias,
    [switch]$ConfigureOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

if (-not (Test-Path $KeystorePath)) {
    Write-Host "Keystore not found: $KeystorePath"
    Write-Host "Check iCloud Drive sync or path."
    exit 1
}

$keytool = "${env:ProgramFiles}\Android\Android Studio\jbr\bin\keytool.exe"
if (-not (Test-Path $keytool)) { $keytool = "keytool" }

if (-not $StorePassword) {
    $secure = Read-Host "Keystore password (input hidden)" -AsSecureString
    $StorePassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}
if (-not $KeyPassword) { $KeyPassword = $StorePassword }

if (-not $Alias) {
    Write-Host "Listing key aliases in keystore..."
    $list = & $keytool -list -keystore $KeystorePath -storepass $StorePassword 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host $list
        Write-Host "Wrong password or keystore error."
        exit 1
    }
    Write-Host $list
    $Alias = Read-Host "Enter keyAlias from the list above (e.g. upload or key0)"
}

$storeFileGradle = $KeystorePath -replace '\\', '/'
$props = @"
storeFile=$storeFileGradle
storePassword=$StorePassword
keyAlias=$Alias
keyPassword=$KeyPassword
"@
Set-Content -Path "keystore.properties" -Value $props -Encoding UTF8
Write-Host "Wrote keystore.properties (gitignored)"
Write-Host "storeFile=$storeFileGradle"
Write-Host "keyAlias=$Alias"

if ($ConfigureOnly) {
    Write-Host ""
    Write-Host "Next: .\build_release_aab.cmd"
    exit 0
}

& "$PSScriptRoot\build_release_aab.ps1"
