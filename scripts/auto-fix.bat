@echo off
title Meeting Record - Auto Fix

echo.
echo ========================================
echo   Meeting Record - Auto Fix Tool
echo ========================================
echo.

echo [Check] PowerShell...
powershell -Command "$PSVersionTable.PSVersion.Major" >nul 2>&1
if errorlevel 1 (
    echo [Error] PowerShell not found
    pause
    exit /b 1
)
echo [OK] PowerShell available

cd /d "%~dp0"

if not exist "auto-fix.ps1" (
    echo [Error] auto-fix.ps1 not found
    pause
    exit /b 1
)

echo [Run] Starting PowerShell script...
echo.

powershell -NoExit -ExecutionPolicy Bypass -Command "& {
    try {
        & '%~dp0auto-fix.ps1' %*
    } catch {
        Write-Host '[Error] ' -ForegroundColor Red -NoNewline
        Write-Host $_.Exception.Message -ForegroundColor Yellow
        Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
    }
    Write-Host ''
    Write-Host 'Press any key to exit...' -ForegroundColor Cyan
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
}"
