$ErrorActionPreference = 'Continue'
$out = @()
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:5173' -TimeoutSec 5 -UseBasicParsing
    $out += "Frontend 5173: OK ($($r.StatusCode))"
} catch { $out += "Frontend 5173: FAIL ($($_.Exception.Message))" }

try {
    $login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"change-me"}' -TimeoutSec 5
    $headers = @{ Authorization = "Bearer $($login.token)" }
    $orgs = Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/organizations' -Method GET -Headers $headers -TimeoutSec 5
    $slugs = ($orgs | ForEach-Object { "$($_.slug):$($_.status)" }) -join ' | '
    $out += "Tenants: $slugs"
} catch { $out += "Tenant list: FAIL ($($_.Exception.Message))" }

$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\final-check.txt -Force -Encoding utf8
Write-Host 'done'