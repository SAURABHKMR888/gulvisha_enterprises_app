$ErrorActionPreference = 'Continue'
$out = @()
try {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/health' -TimeoutSec 5
    $out += "Backend /api/health: OK ($($r.status))"
} catch { $out += "Backend /api/health: FAIL ($($_.Exception.Message))" }

try {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/public/site?slug=abc' -TimeoutSec 5
    $out += "ABC site: $($r.displayName) / $($r.slug)"
} catch { $out += "ABC site: FAIL ($($_.Exception.Message))" }

$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\api-check.txt -Force -Encoding utf8
Write-Host 'done'