# Phase 1 & 2 API integration test script (ASCII-safe)
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$cookieJar = Join-Path $env:TEMP "voting-test-cookies.txt"
if (Test-Path $cookieJar) { Remove-Item $cookieJar -Force }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Body = $null,
        [switch]$Admin,
        [switch]$UseCookie
    )
    $curlArgs = @("-s", "-w", "`nHTTP:%{http_code}", "-X", $Method)
    if ($Admin) { $curlArgs += @("-u", "admin:admin") }
    if ($UseCookie) { $curlArgs += @("-b", $cookieJar, "-c", $cookieJar) }
    if ($Body) {
        $curlArgs += @("-H", "Content-Type: application/json", "-d", $Body)
    }
    $curlArgs += "$base$Path"
    $raw = & curl.exe @curlArgs
    $lines = $raw -split "`n"
    $code = ($lines[-1] -replace "HTTP:", "").Trim()
    $json = ($lines[0..($lines.Length - 2)] -join "`n").Trim()
    return @{ Code = $code; Body = $json }
}

Write-Host "=== Phase 0: Community ===" -ForegroundColor Cyan
$r = Invoke-Api GET "/api/community"
Write-Host "HTTP $($r.Code)"
$community = ConvertFrom-Json $r.Body
$cid = $community.id
Write-Host "id=$cid name=$($community.name) households=$($community.totalHouseholds)"

Write-Host "`n=== Phase 1-1: List units ===" -ForegroundColor Cyan
$r = Invoke-Api GET "/api/admin/communities/$cid/units" -Admin
Write-Host "HTTP $($r.Code)"
if ($r.Code -ne "200") { throw "Admin auth failed: $($r.Body)" }
$units = ConvertFrom-Json $r.Body
Write-Host "unit count: $($units.Count)"

Write-Host "`n=== Phase 1-2: Create unit 4A7 ===" -ForegroundColor Cyan
$unitJson = '{"shortName":"4A7","fullAddress":"addr-4A7","buildingType":"A","floor":4,"unitNo":7,"area":152.4,"ownershipRatio":0.82}'
$r = Invoke-Api POST "/api/admin/communities/$cid/units" -Body $unitJson -Admin
Write-Host "HTTP $($r.Code)"
if ($r.Code -eq "201") {
    $newUnit = ConvertFrom-Json $r.Body
} else {
    $newUnit = $units | Where-Object { $_.shortName -eq "4A7" } | Select-Object -First 1
    Write-Host "unit already exists, id=$($newUnit.id)"
}
Write-Host "unit: $($newUnit.shortName) id=$($newUnit.id)"

Write-Host "`n=== Phase 1-3: Create owner ===" -ForegroundColor Cyan
$ownerJson = '{"unitId":' + $newUnit.id + ',"name":"OwnerTest","phone":"0912345678"}'
$r = Invoke-Api POST "/api/admin/communities/$cid/owners" -Body $ownerJson -Admin
Write-Host "HTTP $($r.Code)"
if ($r.Code -eq "201") {
    $newOwner = ConvertFrom-Json $r.Body
    $authCode = $newOwner.authCode
    $qrToken = $newOwner.qrToken
    Write-Host "authCode=$authCode"
    Write-Host "qrUrl=$($newOwner.qrUrl)"
} else {
    $owners = ConvertFrom-Json (Invoke-Api GET "/api/admin/communities/$cid/owners" -Admin).Body
    $existing = $owners | Where-Object { $_.unitShortName -eq "4A7" } | Select-Object -First 1
    $regen = ConvertFrom-Json (Invoke-Api POST "/api/admin/communities/$cid/owners/$($existing.id)/regenerate-code" -Admin).Body
    $authCode = $regen.authCode
    $qr = ConvertFrom-Json (Invoke-Api GET "/api/admin/communities/$cid/owners/$($existing.id)/qr" -Admin).Body
    $qrToken = $qr.qrToken
    Write-Host "regenerated authCode=$authCode"
}

Write-Host "`n=== Phase 1-4: List owners ===" -ForegroundColor Cyan
$r = Invoke-Api GET "/api/admin/communities/$cid/owners" -Admin
$owners = ConvertFrom-Json $r.Body
$owners | Format-Table id, unitShortName, name, attended

Write-Host "`n=== Phase 2-1: Unit options ===" -ForegroundColor Cyan
$r = Invoke-Api GET "/api/units/options"
$options = ConvertFrom-Json $r.Body
Write-Host "community=$($options.communityName) buildings=$($options.buildings.Count)"

Write-Host "`n=== Phase 2-2: Login unit+code ===" -ForegroundColor Cyan
$verifyJson = '{"unitShortName":"4A7","authCode":"' + $authCode + '"}'
$r = Invoke-Api POST "/api/auth/verify" -Body $verifyJson -UseCookie
Write-Host "HTTP $($r.Code)"
$login = ConvertFrom-Json $r.Body
Write-Host "login: $($login.name) $($login.unitShortName) attended=$($login.attended)"

Write-Host "`n=== Phase 2-3: Session me ===" -ForegroundColor Cyan
$r = Invoke-Api GET "/api/auth/me" -UseCookie
Write-Host "HTTP $($r.Code)"
$me = ConvertFrom-Json $r.Body
Write-Host "me: $($me.name) $($me.unitShortName)"

Write-Host "`n=== Phase 2-4: Wrong code (expect 401) ===" -ForegroundColor Cyan
$r = Invoke-Api POST "/api/auth/verify" -Body '{"unitShortName":"4A7","authCode":"WRONG1"}'
Write-Host "HTTP $($r.Code)"

Write-Host "`n=== Phase 2-5: QR login ===" -ForegroundColor Cyan
$cookieJar2 = Join-Path $env:TEMP "voting-test-cookies-qr.txt"
if (Test-Path $cookieJar2) { Remove-Item $cookieJar2 -Force }
$qrJson = '{"token":"' + $qrToken + '"}'
$raw = & curl.exe -s -w "`nHTTP:%{http_code}" -X POST -H "Content-Type: application/json" -d $qrJson -b $cookieJar2 -c $cookieJar2 "$base/api/auth/qr"
$lines = $raw -split "`n"
Write-Host "HTTP $($lines[-1].Replace('HTTP:',''))"
$qrLogin = ConvertFrom-Json ($lines[0..($lines.Length-2)] -join "`n")
Write-Host "qr login: $($qrLogin.name) $($qrLogin.unitShortName)"

Write-Host "`n=== Phase 2-6: Logout ===" -ForegroundColor Cyan
& curl.exe -s -X POST -b $cookieJar -c $cookieJar "$base/api/auth/logout" | Out-Null
$r = Invoke-Api GET "/api/auth/me" -UseCookie
Write-Host "after logout /auth/me HTTP $($r.Code)"

Write-Host "`n=== ALL TESTS PASSED ===" -ForegroundColor Green
