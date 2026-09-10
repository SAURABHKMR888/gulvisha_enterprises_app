$ErrorActionPreference = 'Continue'
$out = @()
$base = 'http://localhost:8080'

# 1. Login as platform admin
try {
    $login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"change-me"}' -TimeoutSec 5
    $token = $login.token
    $out += "1. Login admin: OK (roles: $($login.roles -join ','))"
} catch {
    $out += "1. Login admin: FAIL ($($_.Exception.Message))"
    $out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\platform-check.txt -Force -Encoding utf8
    Write-Host 'done-fail'
    exit
}
$headers = @{ Authorization = "Bearer $token" }

# 2. List tenants (platform admin endpoint)
try {
    $orgs = Invoke-RestMethod -Uri "$base/api/admin/organizations" -Method GET -Headers $headers -TimeoutSec 5
    $out += "2. List tenants: OK ($($orgs.Count) orgs: $(( $orgs | ForEach-Object { $_.slug } ) -join ', '))"
} catch { $out += "2. List tenants: FAIL ($($_.Exception.Message))" }

# 3. Create a new tenant
try {
    $body = @{
        name = 'XYZ Technologies'
        displayName = 'XYZ Tech'
        slug = 'xyz'
        description = 'Software development & IT consulting'
        industry = 'Technology'
        email = 'info@xyztech.demo'
        primaryColor = '#1a1a2e'
        accentColor = '#e94560'
        adminUsername = 'xyz-admin'
        adminPassword = 'xyz123'
        adminName = 'XYZ Admin'
    } | ConvertTo-Json
    $created = Invoke-RestMethod -Uri "$base/api/admin/organizations" -Method POST -Headers ($headers + @{'Content-Type'='application/json'}) -Body $body -TimeoutSec 5
    $out += "3. Create tenant xyz: OK (id=$($created.id) status=$($created.status))"
} catch { $out += "3. Create tenant xyz: FAIL ($($_.Exception.Message))" }

# 4. Activate it
try {
    $activated = Invoke-RestMethod -Uri "$base/api/admin/organizations/$($created.id)/activate" -Method PATCH -Headers $headers -TimeoutSec 5
    $out += "4. Activate tenant: OK (status=$($activated.status))"
} catch { $out += "4. Activate tenant: FAIL ($($_.Exception.Message))" }

# 5. Public site reflects it
try {
    $site = Invoke-RestMethod -Uri "$base/api/public/site?slug=xyz" -Method GET -TimeoutSec 5
    $out += "5. Public site xyz: OK (displayName=$($site.displayName) color=$($site.primaryColor))"
} catch { $out += "5. Public site xyz: FAIL ($($_.Exception.Message))" }

# 6. Login as new tenant admin
try {
    $login2 = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType 'application/json' -Body '{"username":"xyz-admin","password":"xyz123"}' -TimeoutSec 5
    $out += "6. Login xyz-admin: OK (roles: $($login2.roles -join ','))"
} catch { $out += "6. Login xyz-admin: FAIL ($($_.Exception.Message))" }

$out | Out-File -FilePath E:\AI_LEARNING\gulvisha-enterprises\platform-check.txt -Force -Encoding utf8
Write-Host 'done'