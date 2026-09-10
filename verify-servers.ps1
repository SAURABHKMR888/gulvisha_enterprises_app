$ErrorActionPreference = 'SilentlyContinue'
$backend = $false
$frontend = $false

try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/health' -TimeoutSec 5
    if ($r.StatusCode -eq 200) { $backend = $true }
} catch {}

try {
    $r = Invoke-WebRequest -Uri 'http://localhost:5173' -TimeoutSec 5
    if ($r.StatusCode -eq 200) { $frontend = $true }
} catch {}

Write-Host "Backend (8080): $(if ($backend) { 'OK' } else { 'DOWN' })"
Write-Host "Frontend (5173): $(if ($frontend) { 'OK' } else { 'DOWN' })"

if ($backend -and $frontend) {
    Write-Host 'Both servers running!'
} else {
    Write-Host 'One or both servers are not responding.'
}
