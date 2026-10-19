# ==============================================================================
# TRACENIXA CI/CD REGRESSION QUALITY GATE (PowerShell)
# Automated CI gate script for Windows, local terminals, and Azure DevOps
# Returns exit code 0 if thresholds are satisfied, exit code 1 on regression.
# ==============================================================================

param (
    [string]$BaseUrl = $env:TRACENIXA_URL,
    [string]$ApiKey = $env:TRACENIXA_API_KEY,
    [string]$ProjectId = $env:TRACENIXA_PROJECT_ID,
    [double]$MinPassRate = 80.0,
    [double]$MinFaithfulness = 80.0
)

if (-not $BaseUrl) { $BaseUrl = "http://localhost:8080" }
if (-not $ApiKey) { $ApiKey = "trx_live_demo1234567890abcdef12345678" }
if (-not $ProjectId) { $ProjectId = "11111111-1111-1111-1111-111111111111" }

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "TRACENIXA CI/CD REGRESSION EVALUATION GATE" -ForegroundColor Cyan
Write-Host "Target Platform: $BaseUrl"
Write-Host "Project ID:      $ProjectId"
Write-Host "Thresholds:      Pass Rate >= $MinPassRate%, Faithfulness >= $MinFaithfulness%"
Write-Host "================================================================================" -ForegroundColor Cyan

$payload = @{
    projectId = $ProjectId
    minPassRate = $MinPassRate
    minFaithfulness = $MinFaithfulness
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
    "X-API-Key" = $ApiKey
}

try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api/evaluations/ci-check" -Method Post -Body $payload -Headers $headers -TimeoutSec 30
    
    Write-Host "--------------------------------------------------------------------------------"
    Write-Host "EVALUATION SUMMARY:" -ForegroundColor Green
    Write-Host "Evaluation Run ID:   $($response.runId)"
    Write-Host "Actual Pass Rate:    $($response.actualPassRate)% (Target: >= $MinPassRate%)"
    Write-Host "Actual Faithfulness: $($response.actualFaithfulness)% (Target: >= $MinFaithfulness%)"
    Write-Host "Status Message:      $($response.message)"
    Write-Host "--------------------------------------------------------------------------------"

    if ($response.passed -eq $true) {
        Write-Host ">>> [QUALITY GATE PASSED] Agent meets all regression criteria. Exit code: 0" -ForegroundColor Green
        exit 0
    } else {
        Write-Host ">>> [QUALITY GATE FAILED] Threshold violation detected! Exit code: 1" -ForegroundColor Red
        exit 1
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "CI Gate HTTP Status: $statusCode" -ForegroundColor Red
    Write-Host ">>> [QUALITY GATE FAILED] Regression check rejected with error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

