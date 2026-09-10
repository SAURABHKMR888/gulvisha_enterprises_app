$ErrorActionPreference = 'SilentlyContinue'
# Kill backend on port 8080 only (not VS Code java)
$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen
foreach ($c in $conn) { taskkill /F /PID $c.OwningProcess | Out-Null; Write-Output "killed PID $($c.OwningProcess)" }
Start-Sleep -Seconds 2
Write-Output 'backend stopped'