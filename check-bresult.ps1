$content = [System.IO.File]::ReadAllText('E:\AI_LEARNING\gulvisha-enterprises\build-result.log')
if ($content -match 'BUILD SUCCESS') { Write-Output 'BACKEND BUILD SUCCESS' }
elseif ($content -match 'BUILD FAILURE') { Write-Output 'BACKEND BUILD FAILURE' }
else { Write-Output 'UNKNOWN' }
# last 5 meaningful lines
$lines = $content -split "`n" | Where-Object { $_ -match 'BUILD|ERROR|error:' }
$lines | Select-Object -Last 5