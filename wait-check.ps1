Start-Sleep -Seconds 15
$log = Get-Content E:\AI_LEARNING\gulvisha-enterprises\build-frontend.log -Raw -ErrorAction SilentlyContinue
if ($log -match "built in") { Write-Host "BUILD OK: $log" }
elseif ($log -match "error") { Write-Host "BUILD ERROR: $log" }
else { Write-Host "STILL RUNNING" }