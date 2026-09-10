$ErrorActionPreference = 'Stop'
try {
    $r = Invoke-RestMethod -Uri 'http://localhost:8080/api/public/site?slug=abc' -TimeoutSec 5
    "ABC Endpoint: displayName=$($r.displayName) slug=$($r.slug) primaryColor=$($r.primaryColor)"
} catch {
    "Error: $($_.Exception.Message)"
}
