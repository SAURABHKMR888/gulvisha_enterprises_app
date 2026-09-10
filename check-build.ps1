$log = Get-Content E:\AI_LEARNING\gulvisha-enterprises\build-result.log -Raw
if ($log -match "BUILD SUCCESS") { Write-Host "SUCCESS" } elseif ($log -match "BUILD FAILURE") { Write-Host "FAILURE" } else { Write-Host "STILL BUILDING" }
