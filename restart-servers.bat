@echo off
echo Stopping existing servers...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM node.exe 2>nul
timeout /t 3 /nobreak >nul

echo Starting backend...
start "Backend" java -jar E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar

echo Waiting for backend to start...
timeout /t 12 /nobreak >nul

echo Starting frontend...
cd /d E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend
start "Frontend" npm run dev

echo.
echo ====================================
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo ====================================
echo.
echo Gulvisha login: admin / change-me
echo ABC login:     abc-admin / abc123
echo.
pause
