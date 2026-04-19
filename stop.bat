@echo off
REM Fraud Detection Application Stop Script

echo ============================================
echo   Fraud Detection Application - Stop
echo ============================================
echo.

echo Checking for processes on port 8080...

REM Find and kill processes on port 8080
set FOUND_PROCESS=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo Found process on port 8080 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
    if errorlevel 0 (
        echo Successfully stopped process with PID %%a
        set FOUND_PROCESS=1
    ) else (
        echo Failed to stop process with PID %%a
    )
)

if %FOUND_PROCESS% equ 0 (
    echo No processes found running on port 8080
    echo The application may not be running.
) else (
    echo Application stopped successfully.
)

echo.
pause