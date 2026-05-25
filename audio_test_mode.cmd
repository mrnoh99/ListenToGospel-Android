@echo off
REM Bypasses PowerShell execution policy (no admin required).
REM Usage:
REM   audio_test_mode.cmd -Enable
REM   audio_test_mode.cmd -Restore
REM   audio_test_mode.cmd -Status
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\audio_test_mode.ps1" %*
exit /b %ERRORLEVEL%
