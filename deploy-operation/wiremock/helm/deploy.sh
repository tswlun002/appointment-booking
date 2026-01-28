#!/bin/bash

# WireMock Helm Deployment Script
# Uses Bitnami WireMock Helm chart

set -e

NAMESPACE="appointment-booking"
RELEASE_NAME="wiremock"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================="
echo "  WireMock Helm Deployment (Bitnami)"
echo "=============================================="

# Add Bitnami Helm repository
echo ""
echo "Step 1: Adding Bitnami Helm repository..."
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Create namespace if it doesn't exist
echo ""
echo "Step 2: Creating namespace..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply ConfigMaps for mappings and responses
echo ""
echo "Step 3: Deploying ConfigMaps..."
kubectl apply -f "$SCRIPT_DIR/../configmap-mappings.yaml"
kubectl apply -f "$SCRIPT_DIR/../configmap-responses.yaml"

# Install/Upgrade WireMock using Helm
echo ""
echo "Step 4: Installing WireMock via Bitnami Helm chart..."
helm upgrade --install $RELEASE_NAME bitnami/wiremock \
  --namespace $NAMESPACE \
  --values "$SCRIPT_DIR/values.yaml" \
  --wait \
  --timeout 120s

# Verify deployment
echo ""
echo "Step 5: Verifying deployment..."
kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=wiremock
kubectl get svc -n $NAMESPACE -l app.kubernetes.io/name=wiremock

echo ""
echo "=============================================="
echo "  WireMock Helm Deployment Complete!"
echo "=============================================="
echo ""
echo "WireMock is available at:"
echo "  - Internal: http://$RELEASE_NAME-wiremock.$NAMESPACE.svc.cluster.local:8080"
echo "  - Admin UI: http://$RELEASE_NAME-wiremock.$NAMESPACE.svc.cluster.local:8080/__admin"
echo ""
echo "Available mock endpoints:"
echo "  - Branch Locator: GET /api/v1/branches/search?q={query}"
echo "  - Branch Locator: GET /api/v1/branches/country/{country}"
echo "  - Nager Holidays: GET /api/v3/PublicHolidays/{year}/ZA"
echo "  - Client Domain:  GET /api/v1/clients/{idNumber}"
echo ""
echo "Helm commands:"
echo "  - Status:    helm status $RELEASE_NAME -n $NAMESPACE"
echo "  - Uninstall: helm uninstall $RELEASE_NAME -n $NAMESPACE"
echo "  - Upgrade:   helm upgrade $RELEASE_NAME bitnami/wiremock -n $NAMESPACE -f helm/values.yaml"
echo ""
