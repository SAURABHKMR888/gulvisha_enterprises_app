$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 8080); Write-Host '8080: OPEN' } catch { Write-Host '8080: CLOSED' }
$tcp.Close()
