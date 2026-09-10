Start-Process -FilePath 'java' -ArgumentList '-jar','E:\AI_LEARNING\gulvisha-enterprises\backend\gulvisha-backend\target\gulvisha-backend-0.0.1-SNAPSHOT.jar' -WindowStyle Hidden
Start-Sleep -Seconds 15
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 8080); Write-Output 'Backend 8080: UP' } catch { Write-Output 'Backend 8080: DOWN' }
$tcp.Close()