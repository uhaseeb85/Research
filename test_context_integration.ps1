# Test script for context integration
Write-Host "Testing Context Integration..." -ForegroundColor Green

# Wait for application to start
Write-Host "Waiting for application to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Test 1: Health check
Write-Host "`nTest 1: Health Check" -ForegroundColor Cyan
try {
    $healthResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/health" -Method GET
    Write-Host "Health Check: $healthResponse" -ForegroundColor Green
} catch {
    Write-Host "Health Check Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Context-based authentication
Write-Host "`nTest 2: Context-based Authentication" -ForegroundColor Cyan
$requestBody = @{
    sessionId = "session-12345"
    customerIdentifier = @{
        type = "PHONE_NUMBER"
        value = "+1234567890"
    }
    attemptId = $null
    providedTokens = @()
    brand = "PREMIUM_BANK"
    trustLevelInfo = @{
        trustLevel = "GREEN"
        phoneMatchStatus = "SINGLE_MATCH"
        matchedSsnCount = 1
    }
} | ConvertTo-Json -Depth 3

try {
    $authResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/customer" -Method POST -ContentType "application/json" -Body $requestBody
    Write-Host "Authentication Response:" -ForegroundColor Green
    Write-Host ($authResponse | ConvertTo-Json -Depth 3) -ForegroundColor White
} catch {
    Write-Host "Authentication Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody" -ForegroundColor Red
    }
}

# Test 3: Session context with different session ID
Write-Host "`nTest 3: Different Session Context" -ForegroundColor Cyan
$requestBody2 = @{
    sessionId = "session-67890"
    customerIdentifier = @{
        type = "PHONE_NUMBER"
        value = "+1987654321"
    }
    attemptId = $null
    providedTokens = @()
    brand = "PREMIUM_BANK"
    trustLevelInfo = @{
        trustLevel = "RED"
        phoneMatchStatus = "NOT_MATCHED"
        matchedSsnCount = 0
    }
} | ConvertTo-Json -Depth 3

try {
    $authResponse2 = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/customer" -Method POST -ContentType "application/json" -Body $requestBody2
    Write-Host "Authentication Response 2:" -ForegroundColor Green
    Write-Host ($authResponse2 | ConvertTo-Json -Depth 3) -ForegroundColor White
} catch {
    Write-Host "Authentication 2 Failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nContext Integration Tests Completed!" -ForegroundColor Green 