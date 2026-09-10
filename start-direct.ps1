$ErrorActionPreference = 'Continue'

# Kill anything on 8080
$listeners = netstat -ano | Select-String ':8080 ' | Select-String 'LISTENING'
foreach ($l in $listeners) { $parts = $l.ToString().Trim().Split(/\s+/); taskkill /F /PID $parts[-1] 2>nul | Out-Null }

Write-Host 'Starting backend...'
Start-Process -FilePath 'java' -ArgumentList '-jar','E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar' -WindowStyle Hidden
Write-Host 'Waiting 15s for backend to boot...'
Start-Sleep -Seconds 15

# Check backend
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 8080); Write-Host 'Backend 8080: OK' } catch { Write-Host 'Backend 8080: FAILED' }
$tcp.Close()

Write-Host 'Starting frontend...'
Start-Process -FilePath 'npm' -ArgumentList 'run','dev' -WindowStyle Hidden -WorkingDirectory 'E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend'
Write-Host 'Waiting 8s for frontend...'
Start-Sleep -Seconds 8

$tcp2 = New-Object System.Net.Sockets.TcpClient
try { $tcp2.Connect('localhost', 5173); Write-Host 'Frontend 5173: OK' } catch { Write-Host 'Frontend 5173: FAILED' }
$tcp2.Close()
Write-Host 'DONE'