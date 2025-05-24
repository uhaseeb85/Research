$uri = "http://localhost:8080/api/v1/auth/customer"
$headers = @{
    "Content-Type" = "application/json"
}
$body = @{
    sessionId = "test-session-123"
    customerIdentifier = @{
        type = "PHONE_NUMBER"
        value = "+1234567890"
    }
} | ConvertTo-Json

Write-Host "Testing authentication endpoint..."
Write-Host "Request body: $body"

try {
    $response = Invoke-WebRequest -Uri $uri -Method POST -Headers $headers -Body $body
    Write-Host "Status Code: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        Write-Host "Status Code: $($_.Exception.Response.StatusCode)"
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
} 