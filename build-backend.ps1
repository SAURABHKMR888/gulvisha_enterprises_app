Set-Location E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend
mvn -DskipTests package 2>&1 | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\build-result.log -Force
Write-Host "Build complete. Check build-result.log"
