#!/bin/bash

# WireMock Kubernetes Deployment Script
# This script deploys WireMock to your Kubernetes cluster

set -e

NAMESPACE="appointment-booking"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================="
echo "  WireMock Deployment for Demo/QA Environment"
echo "=============================================="

# Create namespace if it doesn't exist
echo ""
echo "Step 1: Creating namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"

# Apply ConfigMaps
echo ""
echo "Step 2: Deploying ConfigMaps..."
kubectl apply -f "$SCRIPT_DIR/configmap-mappings.yaml"
kubectl apply -f "$SCRIPT_DIR/configmap-responses.yaml"

# Deploy WireMock
echo ""
echo "Step 3: Deploying WireMock..."
kubectl apply -f "$SCRIPT_DIR/deployment.yaml"

# Create Services
echo ""
echo "Step 4: Creating Services..."
kubectl apply -f "$SCRIPT_DIR/service.yaml"

# Wait for deployment to be ready
echo ""
echo "Step 5: Waiting for WireMock to be ready..."
kubectl rollout status deployment/wiremock -n $NAMESPACE --timeout=120s

# Verify deployment
echo ""
echo "Step 6: Verifying deployment..."
kubectl get pods -n $NAMESPACE -l app=wiremock
kubectl get svc -n $NAMESPACE -l app=wiremock

# Test WireMock is responding
echo ""
echo "Step 7: Testing WireMock health..."
WIREMOCK_POD=$(kubectl get pods -n $NAMESPACE -l app=wiremock -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n $NAMESPACE $WIREMOCK_POD -- wget -q -O- http://localhost:8080/__admin/health || echo "Health check endpoint not available (WireMock version may not support it)"

echo ""
echo "=============================================="
echo "  WireMock Deployment Complete!"
echo "=============================================="
echo ""
echo "WireMock is available at:"
echo "  - Internal: http://wiremock.${NAMESPACE}.svc.cluster.local:8080"
echo "  - Admin UI: http://wiremock.${NAMESPACE}.svc.cluster.local:8080/__admin"
echo ""
echo "Available mock endpoints:"
echo "  - Branch Locator: GET /api/v1/branches/search?q={query}"
echo "  - Branch Locator: GET /api/v1/branches/country/{country}"
echo "  - Nager Holidays: GET /api/v3/PublicHolidays/{year}/ZA"
echo "  - Client Domain:  GET /api/v1/clients/{idNumber}"
echo ""
echo "To view mappings:"
echo "  kubectl exec -n $NAMESPACE $WIREMOCK_POD -- wget -q -O- http://localhost:8080/__admin/mappings"
echo ""
