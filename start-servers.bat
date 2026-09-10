@echo off
echo Starting Gulvisha Platform...
echo.

REM Kill existing processes
taskkill /F /IM java.exe 2>nul
taskkill /F /IM node.exe 2>nul
timeout /t 2 /nobreak >nul

REM Start backend
echo Starting backend on port 8080...
start "Backend" java -jar backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar

REM Wait for backend
timeout /t 10 /nobreak >nul

REM Start frontend
echo Starting frontend on port 5173...
cd frontend\gulvisha-frontend
start "Frontend" npm run dev

echo.
echo ========================================
echo  Gulvisha Platform Started!
echo ========================================
echo.
echo  Frontend:  http://localhost:5173
echo  Backend:   http://localhost:8080
echo.
echo  Gulvisha login:  admin / change-me
echo  ABC login:       abc-admin / abc123
echo.
echo ========================================
echo.
echo Press any key to close this window...
pause >nul
