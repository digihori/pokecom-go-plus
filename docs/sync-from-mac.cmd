@echo off
setlocal EnableExtensions DisableDelayedExpansion
rem The PowerShell helper records copied files and removes only former sync files.
if "%~1"=="" goto copy
if /I "%~1"=="/run" goto run
if /I "%~1"=="/preview" goto preview
echo Usage: sync-from-mac.cmd [/preview ^| /run]
exit /b 2
:copy
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync-from-mac.ps1"
exit /b %ERRORLEVEL%
:preview
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync-from-mac.ps1" -Preview
exit /b %ERRORLEVEL%
:run
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync-from-mac.ps1" -Run
exit /b %ERRORLEVEL%
