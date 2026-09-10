Set-Location E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend
npm run build 2>&1 | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\build-frontend.log -Force
Write-Host "Build complete. Check build-frontend.log"
