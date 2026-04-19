@echo off
echo ============================================
echo   Fraud Detection System - Startup
echo ============================================
echo.

echo [1/4] Clearing port 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo [2/4] Building application...
call mvn compile dependency:copy-dependencies -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed! Please check if any other instance is running.
    echo You can run stop.bat to clear the port.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [3/4] Build successful.
echo.
echo [4/4] Starting Application...
echo Dashboard: http://localhost:8080/
echo.

java -cp "target/classes;target/dependency/*" fraud.FraudDetectionApplication
