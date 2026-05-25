@echo off
REM PowerShell: .\use_listentogospel_keystore.cmd
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\use_listentogospel_keystore.ps1" %*
exit /b %ERRORLEVEL%
