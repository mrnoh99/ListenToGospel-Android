@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build_release_aab.ps1" %*
exit /b %ERRORLEVEL%
