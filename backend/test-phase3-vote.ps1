# Phase 3 end-to-end: create proposals, voter login, vote, view results (ASCII-safe)
$ErrorActionPreference = "Stop"
$base = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:8081" }
$adminHeaders = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin")) }
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Post-Json($uri, $body, [switch]$Admin, [switch]$UseSession) {
    $params = @{
        Uri         = $uri
        Method      = "Post"
        ContentType = "application/json; charset=utf-8"
        Body        = ($body | ConvertTo-Json -Compress -Depth 5)
    }
    if ($Admin) { $params.Headers = $adminHeaders }
    if ($UseSession) { $params.WebSession = $session }
    return Invoke-RestMethod @params
}

Write-Host "=== 1. Prepare voter credentials ===" -ForegroundColor Cyan
$community = Invoke-RestMethod -Uri "$base/api/community"
$owners = Invoke-RestMethod -Uri "$base/api/admin/communities/$($community.id)/owners" -Headers $adminHeaders
$owner = $owners | Where-Object { $_.unitShortName -eq "4A7" } | Select-Object -First 1
if (-not $owner) { throw "Owner 4A7 not found. Run test-phase1-2.ps1 first." }
$regen = Post-Json "$base/api/admin/communities/$($community.id)/owners/$($owner.id)/regenerate-code" @{} -Admin
Write-Host "Unit: 4A7 | Auth code: $($regen.authCode)"

Write-Host "`n=== 2. Create test proposals ===" -ForegroundColor Cyan
$p1Body = '{"proposalNumber":"P-001","title":"Exterior repaint","content":"Proposal to repaint building exterior. Budget NT$1.8M.","type":"GENERAL","visible":false,"sortOrder":1}'
$p2Body = '{"proposalNumber":"P-002","title":"EV charging stations","content":"Proposal to install 4 EV chargers in B1 parking.","type":"EXTRAORDINARY","visible":false,"sortOrder":2}'

$existing = Invoke-RestMethod -Uri "$base/api/admin/communities/$($community.id)/proposals" -Headers $adminHeaders
$p1 = $existing | Where-Object { $_.proposalNumber -eq "P-001" } | Select-Object -First 1
$p2 = $existing | Where-Object { $_.proposalNumber -eq "P-002" } | Select-Object -First 1

if (-not $p1) {
    $p1 = Invoke-RestMethod -Uri "$base/api/admin/communities/$($community.id)/proposals" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $p1Body
    Write-Host "Created P-001 id=$($p1.id)"
} else {
    Write-Host "P-001 already exists id=$($p1.id)"
}
if (-not $p2) {
    $p2 = Invoke-RestMethod -Uri "$base/api/admin/communities/$($community.id)/proposals" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $p2Body
    Write-Host "Created P-002 id=$($p2.id)"
} else {
    Write-Host "P-002 already exists id=$($p2.id)"
}

Write-Host "`n=== 3. Start voting (P-001) ===" -ForegroundColor Cyan
$started = Post-Json "$base/api/admin/communities/$($community.id)/proposals/$($p1.id)/start" @{} -Admin
Write-Host "Status: $($started.status) | Visible: $($started.visible)"

Write-Host "`n=== 4. Voter login ===" -ForegroundColor Cyan
$login = Post-Json "$base/api/auth/verify" @{ unitShortName = "4A7"; authCode = $regen.authCode } -UseSession
Write-Host "Logged in as $($login.name) ($($login.unitShortName))"

Write-Host "`n=== 5. List proposals (voter) ===" -ForegroundColor Cyan
$list = Invoke-RestMethod -Uri "$base/api/proposals" -WebSession $session
$list | ForEach-Object { Write-Host "- [$($_.status)] $($_.proposalNumber) $($_.title) voted=$($_.hasVoted)" }

Write-Host "`n=== 6. Submit vote (AGREE on P-001) ===" -ForegroundColor Cyan
if ($p1.hasVoted) {
    Write-Host "Already voted on P-001, skipping vote submit"
} else {
    $vote = Post-Json "$base/api/proposals/$($p1.id)/votes" @{ choice = "AGREE" } -UseSession
    Write-Host "Vote recorded. Total households: $($vote.totalVotedHouseholds) | Passed: $($vote.passed)"
}

Write-Host "`n=== 7. View results ===" -ForegroundColor Cyan
$result = Invoke-RestMethod -Uri "$base/api/proposals/$($p1.id)/results" -WebSession $session
$result.options | ForEach-Object { Write-Host "  $($_.label): $($_.votes) votes, $($_.weight) ping" }

Write-Host "`n=== DONE ===" -ForegroundColor Green
Write-Host "Browser: http://localhost:5173/vote"
Write-Host "Unit: 4A7 | Code: $($regen.authCode)"
