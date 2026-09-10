Start-Sleep -Seconds 20
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 8080); Write-Output 'Backend 8080: UP' } catch { Write-Output 'Backend 8080: DOWN' }
$tcp.Close()