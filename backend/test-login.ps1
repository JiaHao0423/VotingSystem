# Quick voter login test — regenerates auth code and verifies session
$ErrorActionPreference = "Stop"
$base = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:8081" }

$adminCred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin"))
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

$community = Invoke-RestMethod -Uri "$base/api/community"
$owners = Invoke-RestMethod -Uri "$base/api/admin/communities/$($community.id)/owners" -Headers @{ Authorization = "Basic $adminCred" }
$owner = $owners | Where-Object { $_.unitShortName -eq "4A7" } | Select-Object -First 1
if (-not $owner) { throw "No owner for unit 4A7. Run test-phase1-2.ps1 first." }

$regen = Invoke-RestMethod -Uri "$base/api/admin/communities/$($community.id)/owners/$($owner.id)/regenerate-code" -Method Post -Headers @{ Authorization = "Basic $adminCred" }

Write-Host ""
Write-Host "=== Browser login credentials ===" -ForegroundColor Cyan
Write-Host "URL:   http://localhost:5173/vote"
Write-Host "Unit:  4A7"
Write-Host "Code:  $($regen.authCode)"
Write-Host ""

$login = Invoke-RestMethod -Uri "$base/api/auth/verify" -Method Post -WebSession $session `
    -ContentType "application/json; charset=utf-8" `
    -Body (@{ unitShortName = "4A7"; authCode = $regen.authCode } | ConvertTo-Json -Compress)

$me = Invoke-RestMethod -Uri "$base/api/auth/me" -WebSession $session
Write-Host "API login OK: $($me.name) ($($me.unitShortName))" -ForegroundColor Green
