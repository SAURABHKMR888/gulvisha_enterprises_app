Write-Host 'Checking ports...'
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 8080); Write-Host '8080: OPEN' } catch { Write-Host '8080: CLOSED' }
$tcp.Close()
$tcp2 = New-Object System.Net.Sockets.TcpClient
try { $tcp2.Connect('localhost', 5173); Write-Host '5173: OPEN' } catch { Write-Host '5173: CLOSED' }
$tcp2.Close()
