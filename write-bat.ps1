$bat = @'
@echo off
REM =====================================================
REM  Start Gulvisha Platform (backend + frontend)
REM =====================================================

echo [1/3] Stopping any existing backend on port 8080...
@for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr LISTENING') do @taskkill /F /PID %%a 2>nul
timeout /t 2 /nobreak >nul

echo [2/3] Starting Backend (port 8080)...
start "Backend :8080" cmd /k "cd /d E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend && java -jar target\gulvisha-backend-0.0.1-SNAPSHOT.jar"
timeout /t 12 /nobreak >nul

echo [3/3] Starting Frontend (port 5173)...
start "Frontend :5173" cmd /k "cd /d E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend && npm run dev"
timeout /t 6 /nobreak >nul

echo.
echo ========================================
echo  Gulvisha Platform Started!
echo ========================================
echo   Frontend:  http://localhost:5173
echo   Backend:   http://localhost:8080
echo   Gulvisha login:  admin / change-me
echo   ABC login:       abc-admin / abc123
echo   Platform Admin:  login then /platform-admin
echo ========================================
pause
'@
[System.IO.File]::WriteAllText('E:\AI_LEARNING\gulvisha-enterprises\start-servers.bat', $bat, [System.Text.UTF8Encoding]::new($false))
Write-Host "Rewritten start-servers.bat"