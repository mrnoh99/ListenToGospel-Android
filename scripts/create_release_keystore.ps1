# Creates upload-keystore.jks and keystore.properties for Play Store upload.
# Run once from project root. BACK UP the keystore and passwords safely.
#
#   scripts\create_release_keystore.ps1
#   scripts\create_release_keystore.ps1 -StorePassword "your-secret" -KeyPassword "your-secret"

param(
    [string]$StorePassword,
    [string]$KeyPassword,
    [string]$Alias = "upload",
    [string]$KeystoreFile = "upload-keystore.jks"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

$jbrJava = "${env:ProgramFiles}\Android\Android Studio\jbr\bin\keytool.exe"
if (-not (Test-Path $jbrJava)) {
    $jbrJava = "keytool"
}

if (Test-Path $KeystoreFile) {
    Write-Host "Keystore already exists: $KeystoreFile"
    Write-Host "Delete it first if you want to create a new one."
    exit 1
}

if (-not $StorePassword) {
    $secure = Read-Host "Keystore password (input hidden)" -AsSecureString
    $StorePassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}
if (-not $KeyPassword) {
    $KeyPassword = $StorePassword
}

$dname = "CN=ListenToGospel, OU=Mobile, O=mrnoh99, L=Seoul, ST=Seoul, C=KR"
& $jbrJava -genkeypair -v `
    -keystore $KeystoreFile `
    -alias $Alias `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass $StorePassword -keypass $KeyPassword `
    -dname $dname

$props = @"
storeFile=$KeystoreFile
storePassword=$StorePassword
keyAlias=$Alias
keyPassword=$KeyPassword
"@
Set-Content -Path "keystore.properties" -Value $props -Encoding UTF8

Write-Host ""
Write-Host "Created: $KeystoreFile"
Write-Host "Created: keystore.properties (gitignored)"
Write-Host "BACK UP these files and passwords. Loss = cannot update app on Play Store."
Write-Host ""
Write-Host "Next: scripts\build_release_aab.ps1"
