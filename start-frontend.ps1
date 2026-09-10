Start-Process -FilePath 'npm' -ArgumentList 'run','dev' -WindowStyle Hidden -WorkingDirectory 'E:\AI_LEARNING\gulvisha-enterprises\frontend\gulvisha-frontend'
Start-Sleep -Seconds 5
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 5173); Write-Host '5173: OPEN' } catch { Write-Host '5173: CLOSED' }
$tcp.Close()
