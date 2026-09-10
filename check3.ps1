Start-Sleep -Seconds 3
$out = @()
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 8080); $out += '8080: OPEN' } catch { $out += '8080: CLOSED' }
$tcp.Close()
$tcp2 = New-Object System.Net.Sockets.TcpClient
try { $tcp2.Connect('localhost', 5173); $out += '5173: OPEN' } catch { $out += '5173: CLOSED' }
$tcp2.Close()
$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\port-check.txt -Force
