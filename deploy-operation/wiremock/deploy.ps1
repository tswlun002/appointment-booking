# WireMock Kubernetes Deployment Script (PowerShell)
# This script deploys WireMock to your Kubernetes cluster

$ErrorActionPreference = "Stop"

$NAMESPACE = "appointment-booking"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  WireMock Deployment for Demo/QA Environment" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# Create namespace if it doesn't exist
Write-Host ""
Write-Host "Step 1: Creating namespace..." -ForegroundColor Yellow
kubectl apply -f "$SCRIPT_DIR\namespace.yaml"

# Apply ConfigMaps
Write-Host ""
Write-Host "Step 2: Deploying ConfigMaps..." -ForegroundColor Yellow
kubectl apply -f "$SCRIPT_DIR\configmap-mappings.yaml"
kubectl apply -f "$SCRIPT_DIR\configmap-responses.yaml"

# Deploy WireMock
Write-Host ""
Write-Host "Step 3: Deploying WireMock..." -ForegroundColor Yellow
kubectl apply -f "$SCRIPT_DIR\deployment.yaml"

# Create Services
Write-Host ""
Write-Host "Step 4: Creating Services..." -ForegroundColor Yellow
kubectl apply -f "$SCRIPT_DIR\service.yaml"

# Wait for deployment to be ready
Write-Host ""
Write-Host "Step 5: Waiting for WireMock to be ready..." -ForegroundColor Yellow
kubectl rollout status deployment/wiremock -n $NAMESPACE --timeout=120s

# Verify deployment
Write-Host ""
Write-Host "Step 6: Verifying deployment..." -ForegroundColor Yellow
kubectl get pods -n $NAMESPACE -l app=wiremock
kubectl get svc -n $NAMESPACE -l app=wiremock

# Test WireMock is responding
Write-Host ""
Write-Host "Step 7: Testing WireMock health..." -ForegroundColor Yellow
$WIREMOCK_POD = kubectl get pods -n $NAMESPACE -l app=wiremock -o jsonpath='{.items[0].metadata.name}'
try {
    kubectl exec -n $NAMESPACE $WIREMOCK_POD -- wget -q -O- http://localhost:8080/__admin/health
} catch {
    Write-Host "Health check endpoint not available (WireMock version may not support it)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  WireMock Deployment Complete!" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "WireMock is available at:" -ForegroundColor White
Write-Host "  - Internal: http://wiremock.$NAMESPACE.svc.cluster.local:8080" -ForegroundColor White
Write-Host "  - Admin UI: http://wiremock.$NAMESPACE.svc.cluster.local:8080/__admin" -ForegroundColor White
Write-Host ""
Write-Host "Available mock endpoints:" -ForegroundColor White
Write-Host "  - Branch Locator: GET /api/v1/branches/search?q={query}" -ForegroundColor White
Write-Host "  - Branch Locator: GET /api/v1/branches/country/{country}" -ForegroundColor White
Write-Host "  - Nager Holidays: GET /api/v3/PublicHolidays/{year}/ZA" -ForegroundColor White
Write-Host "  - Client Domain:  GET /api/v1/clients/{idNumber}" -ForegroundColor White
Write-Host ""
Write-Host "To view mappings:" -ForegroundColor White
Write-Host "  kubectl exec -n $NAMESPACE $WIREMOCK_POD -- wget -q -O- http://localhost:8080/__admin/mappings" -ForegroundColor White
Write-Host ""
