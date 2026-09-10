@echo off
echo Stopping any existing servers...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM node.exe 2>nul
timeout /t 2 /nobreak > nul

echo.
echo Starting Backend (port 8080)...
start "Backend" java -jar E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar

echo Waiting 10 seconds for backend...
timeout /t 10 /nobreak > nul

echo.
echo Starting Frontend (port 5173)...
start "Frontend" cmd /c "cd /d E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend && npm run dev"

echo.
echo Both servers starting. Check the new windows.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
pause
