Start-Sleep -Seconds 5
$out = @()
$tcp = New-Object System.Net.Sockets.TcpClient
try { $tcp.Connect('localhost', 5173); $out += '5173: OPEN' } catch { $out += '5173: CLOSED' }
$tcp.Close()
$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\port-check.txt -Force
