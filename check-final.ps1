$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 5173); Write-Host '5173: OPEN' } catch { Write-Host '5173: CLOSED' }
$tcp.Close()
