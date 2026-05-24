# Build launcher icons from religious-crosses EPS companion JPG (solid cross, left half).
# Source: iCloudDrive/.../14f3d867-e467-4003-a205-229b0d21d671.jpg (8000x8000)
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts/generate-android-icons.ps1

param(
    [string]$SourceJpg = "c:\Users\jsnoh\iCloudDrive\AppDevelop\religious-crosses-outline-glyph\14f3d867-e467-4003-a205-229b0d21d671.jpg",
    [double]$ArtScale = 0.66
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path $PSScriptRoot -Parent
$resBase = Join-Path $repoRoot "app\src\main\res"

$Navy = [System.Drawing.Color]::FromArgb(255, 14, 48, 112)
$White = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
$NavyNight = [System.Drawing.Color]::FromArgb(255, 10, 30, 69)

if (-not (Test-Path $SourceJpg)) {
    throw "Source not found: $SourceJpg"
}

function Save-Png([System.Drawing.Image]$image, [string]$path) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    if (Test-Path $path) { Remove-Item $path -Force }
    $image.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function Get-ContentBounds([System.Drawing.Bitmap]$bmp, [int]$threshold) {
    $minX = $bmp.Width; $minY = $bmp.Height; $maxX = 0; $maxY = 0
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $p = $bmp.GetPixel($x, $y)
            if ($p.R -lt $threshold) {
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    if ($maxX -le $minX) { return $null }
  return @{ MinX = $minX; MinY = $minY; MaxX = $maxX; MaxY = $maxY }
}

function New-IconFromCrossJpg([System.Drawing.Image]$source, [int]$outSize, [System.Drawing.Color]$background, [double]$artScale) {
    # Sheet has solid (left) + outline (right) — use left half only.
    $halfW = [int]($source.Width / 2)
    $crop = New-Object Drawing.Bitmap $halfW, $source.Height
    $g = [Drawing.Graphics]::FromImage($crop)
    $g.DrawImage($source, [Drawing.Rectangle]::new(0, 0, $halfW, $source.Height),
        0, 0, $halfW, $source.Height, [Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()

  # Downscale for bounds scan (faster)
    $scan = 400
    $scanH = [int]($crop.Height * ($scan / $crop.Width))
    $small = New-Object Drawing.Bitmap $scan, $scanH
    $gs = [Drawing.Graphics]::FromImage($small)
    $gs.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gs.DrawImage($crop, 0, 0, $scan, $scanH)
    $gs.Dispose()

    $b = Get-ContentBounds $small 180
    if ($null -eq $b) { $crop.Dispose(); $small.Dispose(); throw "No cross shape found in source." }

    $pad = 8
    $sx = ($crop.Width / $scan)
    $sy = ($crop.Height / $scanH)
    $x0 = [Math]::Max(0, [int](($b.MinX - $pad) * $sx))
    $y0 = [Math]::Max(0, [int](($b.MinY - $pad) * $sy))
    $x1 = [Math]::Min($crop.Width - 1, [int](($b.MaxX + $pad) * $sx))
    $y1 = [Math]::Min($crop.Height - 1, [int](($b.MaxY + $pad) * $sy))
    $cw = $x1 - $x0 + 1
    $ch = $y1 - $y0 + 1

    $trim = New-Object Drawing.Bitmap $cw, $ch
    $gt = [Drawing.Graphics]::FromImage($trim)
    $gt.DrawImage($crop, [Drawing.Rectangle]::new(0, 0, $cw, $ch),
        $x0, $y0, $cw, $ch, [Drawing.GraphicsUnit]::Pixel)
    $gt.Dispose()
    $crop.Dispose()
    $small.Dispose()

    # Black glyph on white -> white cross on navy
    $silhouette = New-Object Drawing.Bitmap $cw, $ch
    for ($y = 0; $y -lt $ch; $y++) {
        for ($x = 0; $x -lt $cw; $x++) {
            $p = $trim.GetPixel($x, $y)
            $silhouette.SetPixel($x, $y, $(if ($p.R -lt 180) { $White } else { $background }))
        }
    }
    $trim.Dispose()

    $out = New-Object Drawing.Bitmap $outSize, $outSize
    $go = [Drawing.Graphics]::FromImage($out)
    $go.Clear($background)
    $go.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

    $draw = [int]($outSize * $artScale)
    $ox = ($outSize - $draw) / 2
    $oy = ($outSize - $draw) / 2
    $go.DrawImage($silhouette, $ox, $oy, $draw, $draw)
    $go.Dispose()
    $silhouette.Dispose()
    return $out
}

function Write-LegacyMipmaps([System.Drawing.Image]$source, [System.Drawing.Color]$background, [string]$prefix) {
    $sizes = @{ "${prefix}-mdpi"=48; "${prefix}-hdpi"=72; "${prefix}-xhdpi"=96; "${prefix}-xxhdpi"=144; "${prefix}-xxxhdpi"=192 }
    foreach ($folder in $sizes.Keys) {
        $px = $sizes[$folder]
        $icon = New-IconFromCrossJpg $source $px $background 1.0
        $dir = Join-Path $resBase $folder
        Save-Png $icon (Join-Path $dir "ic_launcher.png")
        Save-Png $icon (Join-Path $dir "ic_launcher_round.png")
        $icon.Dispose()
        Write-Host "  $folder ${px}px"
    }
}

Write-Host "Source: $SourceJpg"
$src = [Drawing.Image]::FromFile($SourceJpg)

$fg = New-IconFromCrossJpg $src 1024 $Navy $ArtScale
Save-Png $fg (Join-Path $resBase "drawable-nodpi\ic_launcher_art.png")
$fg.Dispose()
Write-Host "Wrote drawable-nodpi/ic_launcher_art.png"

$fgNight = New-IconFromCrossJpg $src 1024 $NavyNight $ArtScale
Save-Png $fgNight (Join-Path $resBase "drawable-night-nodpi\ic_launcher_art.png")
$fgNight.Dispose()
Write-Host "Wrote drawable-night-nodpi/ic_launcher_art.png"

$mono = New-IconFromCrossJpg $src 1024 $Navy $ArtScale
Save-Png $mono (Join-Path $resBase "drawable-nodpi\ic_launcher_monochrome_art.png")
$mono.Dispose()
Write-Host "Wrote drawable-nodpi/ic_launcher_monochrome_art.png"

Write-Host "Legacy mipmaps:"
Write-LegacyMipmaps $src $Navy "mipmap"
Write-LegacyMipmaps $src $NavyNight "mipmap-night"

$src.Dispose()
Write-Host "Done. Tune size: -ArtScale 0.62 (smaller) or 0.70 (larger)"
