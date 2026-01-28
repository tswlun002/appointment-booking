# WireMock Helm Deployment Script (PowerShell)
# Uses Bitnami WireMock Helm chart

$ErrorActionPreference = "Stop"

$NAMESPACE = "appointment-booking"
$RELEASE_NAME = "wiremock"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  WireMock Helm Deployment (Bitnami)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# Add Bitnami Helm repository
Write-Host ""
Write-Host "Step 1: Adding Bitnami Helm repository..." -ForegroundColor Yellow
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Create namespace if it doesn't exist
Write-Host ""
Write-Host "Step 2: Creating namespace..." -ForegroundColor Yellow
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply ConfigMaps for mappings and responses
Write-Host ""
Write-Host "Step 3: Deploying ConfigMaps..." -ForegroundColor Yellow
kubectl apply -f "$SCRIPT_DIR\..\configmap-mappings.yaml"
kubectl apply -f "$SCRIPT_DIR\..\configmap-responses.yaml"

# Install/Upgrade WireMock using Helm
Write-Host ""
Write-Host "Step 4: Installing WireMock via Bitnami Helm chart..." -ForegroundColor Yellow
helm upgrade --install $RELEASE_NAME bitnami/wiremock `
  --namespace $NAMESPACE `
  --values "$SCRIPT_DIR\values.yaml" `
  --wait `
  --timeout 120s

# Verify deployment
Write-Host ""
Write-Host "Step 5: Verifying deployment..." -ForegroundColor Yellow
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=wiremock
kubectl get svc -n $NAMESPACE -l app.kubernetes.io/name=wiremock

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "  WireMock Helm Deployment Complete!" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "WireMock is available at:" -ForegroundColor White
Write-Host "  - Internal: http://$RELEASE_NAME-wiremock.$NAMESPACE.svc.cluster.local:8080" -ForegroundColor White
Write-Host "  - Admin UI: http://$RELEASE_NAME-wiremock.$NAMESPACE.svc.cluster.local:8080/__admin" -ForegroundColor White
Write-Host ""
Write-Host "Available mock endpoints:" -ForegroundColor White
Write-Host "  - Branch Locator: GET /api/v1/branches/search?q={query}" -ForegroundColor White
Write-Host "  - Branch Locator: GET /api/v1/branches/country/{country}" -ForegroundColor White
Write-Host "  - Nager Holidays: GET /api/v3/PublicHolidays/{year}/ZA" -ForegroundColor White
Write-Host "  - Client Domain:  GET /api/v1/clients/{idNumber}" -ForegroundColor White
Write-Host ""
Write-Host "Helm commands:" -ForegroundColor White
Write-Host "  - Status:    helm status $RELEASE_NAME -n $NAMESPACE" -ForegroundColor Gray
Write-Host "  - Uninstall: helm uninstall $RELEASE_NAME -n $NAMESPACE" -ForegroundColor Gray
Write-Host "  - Upgrade:   helm upgrade $RELEASE_NAME bitnami/wiremock -n $NAMESPACE -f helm/values.yaml" -ForegroundColor Gray
Write-Host ""
