$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$abcTok = (Get-Content "E:\AI_LEARNING\gulvisha-enterprises\abc-token.json" -Raw | ConvertFrom-Json).token

$restore = @{
  name = "ABC Consulting LLP"
  displayName = "ABC Consulting"
  slug = "abc"
  logoUrl = $null
  faviconUrl = $null
  primaryColor = "#1e3a5f"
  accentColor = "#d4a843"
  description = "Accounting, Tax & Business Advisory for SMEs"
  industry = "Professional Services"
  email = "info@abcconsulting.com"
  phone = $null
  website = "https://abcconsulting.demo"
  address = $null
  timezone = $null
  currency = $null
  language = $null
  siteContent = '{"heroEyebrow":"ACCOUNTING · TAX · ADVISORY","heroTitle":"{brandName} — clarity for your numbers and your decisions.","heroSubheading":"Accounting, Tax & Business Advisory for SMEs","aboutHeading":"Why ABC Consulting?","aboutCapabilities":["Statutory audit & assurance","Direct & indirect tax compliance","Business advisory & CFO services","Company secretarial services","Payroll & compliance management","Management consulting"],"industries":["Manufacturing","Trading companies","Startups & SMEs","Professional services","Real estate","Healthcare practices","IT & software companies"],"processHeading":"Our engagement process","processSteps":[{"title":"Understand your books","description":"We review your existing financial records, processes, and pain points."},{"title":"Plan the engagement","description":"We propose a clear scope, timeline, and deliverables tailored to your needs."},{"title":"Execute with precision","description":"Our qualified professionals deliver accurate, compliant work on schedule."},{"title":"Advise as you grow","description":"Ongoing advisory support to help you make better business decisions."}],"quoteHeading":"Request a consultation","quoteSubheading":"Let''s discuss your compliance and advisory needs.","quoteDescription":"Tell us about your business and what financial clarity you are looking for."}'
} | ConvertTo-Json -Compress

Set-Content -Path "E:\AI_LEARNING\gulvisha-enterprises\abc-restore.json" -Value $restore -NoNewline -Encoding utf8
$resp = curl.exe -s -w "`n%{http_code}" -X PUT "$base/api/organizations/current" -H "Authorization: Bearer $abcTok" -H "Content-Type: application/json" --data "@E:\AI_LEARNING\gulvisha-enterprises\abc-restore.json"
Write-Host "RESTORE status (last line):"
($resp -split "`n")[-1]