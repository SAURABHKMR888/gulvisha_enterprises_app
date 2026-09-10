$ErrorActionPreference = 'Continue'

Write-Host 'Starting backend...'
Start-Process -FilePath 'java' -ArgumentList '-jar','E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar' -WindowStyle Hidden

Write-Host 'Waiting 10s for backend...'
Start-Sleep -Seconds 10

Write-Host 'Starting frontend...'
Start-Process -FilePath 'npm' -ArgumentList 'run','dev' -WindowStyle Hidden -WorkingDirectory 'E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend'

Write-Host 'Waiting 5s for frontend...'
Start-Sleep -Seconds 5

Write-Host 'Checking ports...'
$netstat = netstat -an
$backend = $netstat | Select-String ':8080.*LISTENING'
$frontend = $netstat | Select-String ':5173.*LISTENING'

if ($backend) { Write-Host 'Backend: OK' } else { Write-Host 'Backend: NOT LISTENING' }
if ($frontend) { Write-Host 'Frontend: OK' } else { Write-Host 'Frontend: NOT LISTENING' }

Write-Host 'Done!'
