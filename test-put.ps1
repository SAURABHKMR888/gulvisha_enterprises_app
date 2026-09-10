$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$abcTok = (Get-Content "E:\AI_LEARNING\gulvisha-enterprises\abc-token.json" -Raw | ConvertFrom-Json).token

# Minimal valid update simulating what the new Settings UI sends (siteContent as JSON string)
$update = @{
  name = "ABC Consulting LLP"
  displayName = "ABC Consulting PRO"
  slug = "abc"
  logoUrl = "https://example.com/abc-logo.png"
  faviconUrl = $null
  primaryColor = "#0f2f52"
  accentColor = "#e0a832"
  description = "Accounting & Tax"
  industry = "Professional Services"
  email = "info@abcconsulting.com"
  phone = "+1 555 1234"
  website = "https://abcconsulting.demo"
  address = "12 Queen St"
  timezone = $null
  currency = "GBP"
  language = "en"
  siteContent = '{"heroEyebrow":"ACCOUNTING TAX ADVISORY","heroTitle":"{brandName} PRO","heroSubheading":"Accounting for SMEs","aboutHeading":"About ABC","aboutCapabilities":["Audit","Tax"],"industries":["Retail","Tech"],"processHeading":"Process","processSteps":[{"title":"Step1"}],"quoteHeading":"Get in touch"}'
} | ConvertTo-Json -Compress

Set-Content -Path "E:\AI_LEARNING\gulvisha-enterprises\abc-update.json" -Value $update -NoNewline -Encoding utf8

$resp = curl.exe -s -w "`n%{http_code}" -X PUT "$base/api/organizations/current" -H "Authorization: Bearer $abcTok" -H "Content-Type: application/json" --data "@E:\AI_LEARNING\gulvisha-enterprises\abc-update.json"
Write-Host "PUT RESULT (status at end):"
Write-Host $resp
Write-Host ""
Write-Host "PUBLIC SITE AFTER:"
$site = curl.exe -s "$base/api/public/site?slug=abc"
$siteJson = $site | ConvertFrom-Json
Write-Host "displayName=$($siteJson.displayName) primaryColor=$($siteJson.primaryColor) accentColor=$($siteJson.accentColor) currency-ish=$($siteJson.siteContent.quoteHeading)"