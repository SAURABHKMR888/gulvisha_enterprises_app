$out = @()
Get-Process -Name java -ErrorAction SilentlyContinue | ForEach-Object { $out += "java PID=$($_.Id) MEM=$([math]::Round($_.WorkingSet64/1MB,0))MB" }
Get-Process -Name node -ErrorAction SilentlyContinue | ForEach-Object { $out += "node PID=$($_.Id) MEM=$([math]::Round($_.WorkingSet64/1MB,0))MB" }
if ($out.Count -eq 0) { $out += "NO java/node processes" }
$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\proc-check.txt -Force -Encoding utf8
Write-Host "done"