# Shortens all chapter M4A files to ~N seconds for testing auto-advance between chapters.
# Originals are kept in .audio-backup/ at the repo root (gitignored).
#
#   audio_test_mode.cmd -Enable                     # recommended on Windows (no execution policy)
#   .\scripts\audio_test_mode.ps1 -Enable          # or: powershell -ExecutionPolicy Bypass -File ...
#   .\scripts\audio_test_mode.ps1 -Restore
#   .\scripts\audio_test_mode.ps1 -Enable -Seconds 8
#   .\scripts\audio_test_mode.ps1 -Status

param(
    [switch]$Enable,
    [switch]$Restore,
    [switch]$Status,
    [int]$Seconds = 10
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path $PSScriptRoot -Parent
$AudioDir = Join-Path $ProjectRoot "app\src\main\assets\AudioFiles"
$BackupDir = Join-Path $ProjectRoot ".audio-backup"
$MarkerFile = Join-Path $ProjectRoot ".audio-test-mode"

function Get-FfmpegPath {
    $cmd = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $common = @(
        "${env:ProgramFiles}\ffmpeg\bin\ffmpeg.exe",
        "${env:ProgramFiles(x86)}\ffmpeg\bin\ffmpeg.exe",
        "$env:LOCALAPPDATA\Microsoft\WinGet\Links\ffmpeg.exe"
    )
    foreach ($path in $common) {
        if (Test-Path $path) { return $path }
    }
    return $null
}

function Get-M4aFiles([string]$Root) {
    if (-not (Test-Path $Root)) { return @() }
    return Get-ChildItem -Path $Root -Recurse -Filter "*.m4a" -File
}

function Show-Status {
    $active = Get-M4aFiles $AudioDir
    $backup = Get-M4aFiles $BackupDir
    $mode = if (Test-Path $MarkerFile) { "TEST (short clips)" } else { "NORMAL (or unknown)" }
    Write-Host "Mode: $mode"
    Write-Host "Active audio: $($active.Count) file(s) in app/src/main/assets/AudioFiles"
    Write-Host "Backup:       $($backup.Count) file(s) in .audio-backup"
    if (Test-Path $MarkerFile) {
        Write-Host "Marker: $(Get-Content $MarkerFile -Raw)"
    }
}

function Enable-ShortAudio {
    if (-not (Test-Path $AudioDir)) {
        throw "Audio folder not found: $AudioDir`nRun copy_audio_assets.sh first."
    }

    $ffmpeg = Get-FfmpegPath
    if (-not $ffmpeg) {
        throw @"
ffmpeg not found. Install it, then reopen PowerShell.
  winget install Gyan.FFmpeg
  choco install ffmpeg
"@
    }

    $sources = Get-M4aFiles $AudioDir
    if ($sources.Count -eq 0) {
        throw "No .m4a files under $AudioDir"
    }

    if (-not (Test-Path $BackupDir)) {
        Write-Host "Backing up $($sources.Count) file(s) to .audio-backup ..."
        New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
        $audioResolved = (Resolve-Path $AudioDir).Path
        foreach ($file in $sources) {
            $relative = $file.FullName.Substring($audioResolved.Length).TrimStart('\')
            $dest = Join-Path $BackupDir $relative
            $destDir = Split-Path $dest -Parent
            if (-not (Test-Path $destDir)) {
                New-Item -ItemType Directory -Path $destDir -Force | Out-Null
            }
            Copy-Item -Path $file.FullName -Destination $dest -Force
        }
    } else {
        Write-Host "Using existing .audio-backup (originals not re-copied)."
    }

    $backupSources = Get-M4aFiles $BackupDir
    if ($backupSources.Count -eq 0) {
        throw "Backup folder is empty: $BackupDir"
    }

    Write-Host "Creating ~${Seconds}s test clips from backup ($($backupSources.Count) files) ..."
    $backupResolved = (Resolve-Path $BackupDir).Path
    $index = 0
    foreach ($source in $backupSources) {
        $index++
        $relative = $source.FullName.Substring($backupResolved.Length).TrimStart('\')
        $dest = Join-Path $AudioDir $relative
        $destDir = Split-Path $dest -Parent
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }

        $tempOut = "$dest.tmp.m4a"
        if (Test-Path $tempOut) { Remove-Item $tempOut -Force }

        $args = @(
            "-hide_banner", "-loglevel", "error", "-y",
            "-i", $source.FullName,
            "-t", "$Seconds",
            "-c:a", "aac",
            "-b:a", "96k",
            "-movflags", "+faststart",
            $tempOut
        )
        & $ffmpeg @args
        if ($LASTEXITCODE -ne 0) {
            if (Test-Path $tempOut) { Remove-Item $tempOut -Force }
            throw "ffmpeg failed for $($source.Name)"
        }

        Move-Item -Path $tempOut -Destination $dest -Force
        Write-Host "  [$index/$($backupSources.Count)] $relative"
    }

    @(
        "enabled_at=$(Get-Date -Format o)",
        "seconds=$Seconds",
        "file_count=$($backupSources.Count)"
    ) | Set-Content -Path $MarkerFile -Encoding UTF8

    Write-Host ""
    Write-Host "Done. Test mode ON (~${Seconds}s per chapter)."
    Write-Host "Rebuild/run the app, then test chapter auto-advance."
    Write-Host "Restore originals: .\scripts\audio_test_mode.ps1 -Restore"
}

function Restore-FullAudio {
    if (-not (Test-Path $BackupDir)) {
        throw "No backup found at $BackupDir. Cannot restore."
    }

    $backupFiles = Get-M4aFiles $BackupDir
    if ($backupFiles.Count -eq 0) {
        throw "Backup folder has no .m4a files."
    }

    Write-Host "Restoring $($backupFiles.Count) original file(s) ..."
    if (-not (Test-Path $AudioDir)) {
        New-Item -ItemType Directory -Path $AudioDir -Force | Out-Null
    }

    $backupResolved = (Resolve-Path $BackupDir).Path
    foreach ($source in $backupFiles) {
        $relative = $source.FullName.Substring($backupResolved.Length).TrimStart('\')
        $dest = Join-Path $AudioDir $relative
        $destDir = Split-Path $dest -Parent
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        Copy-Item -Path $source.FullName -Destination $dest -Force
    }

    if (Test-Path $MarkerFile) {
        Remove-Item $MarkerFile -Force
    }

    Write-Host "Done. Full audio restored."
    Write-Host "Optional: remove backup folder to free disk space: Remove-Item -Recurse -Force .audio-backup"
}

$selected = @($Enable.IsPresent, $Restore.IsPresent, $Status.IsPresent) | Where-Object { $_ }
if ($selected.Count -ne 1) {
    Write-Host @"
Usage:
  .\scripts\audio_test_mode.ps1 -Enable
  .\scripts\audio_test_mode.ps1 -Restore
  .\scripts\audio_test_mode.ps1 -Status
  .\scripts\audio_test_mode.ps1 -Enable -Seconds 8
"@
    exit 1
}

if ($Status) {
    Show-Status
    exit 0
}

if ($Enable) {
    Enable-ShortAudio
    exit 0
}

Restore-FullAudio
