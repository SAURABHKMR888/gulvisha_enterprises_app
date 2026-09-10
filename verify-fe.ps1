$out = @()
# Confirm dev server is serving the current App.tsx (contains platform-admin route)
try {
    $main = Invoke-WebRequest -Uri 'http://localhost:5173/src/App.tsx' -TimeoutSec 5 -UseBasicParsing
    if ($main.Content -match 'platform-admin') { $out += 'Dev server serves platform-admin route: OK' }
    else { $out += 'Dev server App.tsx MISSING platform-admin' }
} catch { $out += "App.tsx fetch: FAIL ($($_.Exception.Message))" }
$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\fe-verify.txt -Force -Encoding utf8
Write-Host 'done'