# Captures an Android device screenshot via ADB and exports a Play Console ready image.
#
# Usage (run from repo root):
#   powershell -ExecutionPolicy Bypass -File ".\scripts\capture_play_screenshot.ps1" -Name "01-home"
#   powershell -ExecutionPolicy Bypass -File ".\scripts\capture_play_screenshot.ps1" -Name "02-playing" -NoCrop
#
# Output:
#   play-store-assets\phone-screenshots-android\<Name>-1080x1920.jpg
#
param(
    [Parameter(Mandatory = $true)]
    [string]$Name,

    # If set, does not crop/resize; just pulls the raw PNG from device.
    [switch]$NoCrop
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path $PSScriptRoot -Parent
$OutDir = Join-Path $ProjectRoot "play-store-assets\phone-screenshots-android"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

function Ensure-Device {
    $out = & $adb devices 2>&1
    $lines = @($out | ForEach-Object { "$_".Trim() }) | Where-Object { $_ -ne "" }

    # Typical line examples:
    #   R3CN20PN7JN            device product:...
    #   R3CN20PN7JN            unauthorized
    $hasAuthorized = $false
    $hasUnauthorized = $false
    foreach ($line in $lines) {
        if ($line -match "^\S+\s+device(\s|$)") { $hasAuthorized = $true }
        if ($line -match "^\S+\s+unauthorized(\s|$)") { $hasUnauthorized = $true }
    }

    if ($hasAuthorized) { return }

    if ($hasUnauthorized) {
        throw "Connected device is unauthorized. Unlock phone and tap 'Allow USB debugging', then retry. adb output: $($lines -join ' | ')"
    }

    throw "No Android device found. Connect phone and ensure 'adb devices' shows '<serial> device'. adb output: $($lines -join ' | ')"
}

function Capture-RawPng([string]$destPngPath) {
    $remote = "/sdcard/__play_shot.png"
    & $adb shell screencap -p $remote | Out-Null
    & $adb pull $remote $destPngPath | Out-Null
    & $adb shell rm $remote | Out-Null
}

Ensure-Device

$safe = ($Name -replace '[^A-Za-z0-9_\-\.]', '_')
$rawPng = Join-Path $OutDir ("$safe-raw.png")
Capture-RawPng $rawPng
Write-Host "Captured: $rawPng"

if ($NoCrop) {
    Write-Host "Done (NoCrop)."
    exit 0
}

Add-Type -AssemblyName System.Drawing

$img = [System.Drawing.Image]::FromFile($rawPng)
try {
    $srcW = $img.Width
    $srcH = $img.Height

    # Target aspect: 9:16 portrait.
    $targetRatio = 9.0 / 16.0
    $srcRatio = $srcW / [double]$srcH

    if ($srcRatio -gt $targetRatio) {
        # too wide -> crop width
        $cropH = $srcH
        $cropW = [int][Math]::Round($srcH * $targetRatio)
        $cropX = [int][Math]::Round(($srcW - $cropW) / 2.0)
        $cropY = 0
    } else {
        # too tall -> crop height
        $cropW = $srcW
        $cropH = [int][Math]::Round($srcW / $targetRatio)
        $cropX = 0
        $cropY = [int][Math]::Round(($srcH - $cropH) / 2.0)
    }

    $dstW = 1080
    $dstH = 1920
    $bmp = New-Object System.Drawing.Bitmap $dstW, $dstH, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.Clear([System.Drawing.Color]::White)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

        $destRect = New-Object System.Drawing.Rectangle 0, 0, $dstW, $dstH
        $srcRect = New-Object System.Drawing.Rectangle $cropX, $cropY, $cropW, $cropH
        $g.DrawImage($img, $destRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)

        $jpgPath = Join-Path $OutDir ("$safe-1080x1920.jpg")
        $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" } | Select-Object -First 1
        $encParams = New-Object System.Drawing.Imaging.EncoderParameters 1
        $encParams.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, 92L)
        $bmp.Save($jpgPath, $codec, $encParams)
        Write-Host "Created: $jpgPath"
    } finally {
        $g.Dispose()
        $bmp.Dispose()
    }
} finally {
    $img.Dispose()
}

