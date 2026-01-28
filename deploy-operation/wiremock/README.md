# WireMock Standalone for Demo/Integration/QA Environments

This directory contains the WireMock configuration for mocking external services in non-production environments.

## Mocked Services

| Service | Purpose | Used By |
|---------|---------|---------|
| **Branch Locator** | Provides branch information and operation hours | Location context |
| **Nager Holidays** | Provides South African public holidays | Shared-kernel Day context |
| **Client Domain** | Provides Capitec client information by ID number | User context |

## Directory Structure

```
deploy-operation/wiremock/
├── helm/                        # Helm deployment (Recommended)
│   ├── values.yaml              # Helm values configuration
│   ├── deploy.sh                # Bash deployment script
│   └── deploy.ps1               # PowerShell deployment script
├── Dockerfile                   # Docker image for standalone deployment
├── namespace.yaml               # Kubernetes namespace
├── configmap-mappings.yaml      # WireMock request mappings (ConfigMap)
├── configmap-responses.yaml     # WireMock response files (ConfigMap)
├── deployment.yaml              # Kubernetes deployment (manual)
├── service.yaml                 # Kubernetes service (manual)
├── kustomization.yaml           # Kustomize configuration
└── responses/                   # Standalone response files
    ├── mappings/
    │   ├── branch-locator/
    │   ├── nager-holidays/
    │   └── client-domain/
    └── __files/
        ├── branch-locator/
        ├── nager-holidays/
        └── client-domain/
```

## Deployment Options

### Option 1: Helm Chart - Bitnami (Recommended)

Uses the Bitnami WireMock Helm chart for easier management.

```bash
# Using bash script
./helm/deploy.sh

# Or using PowerShell
./helm/deploy.ps1

# Or manually
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

kubectl create namespace appointment-booking
kubectl apply -f configmap-mappings.yaml
kubectl apply -f configmap-responses.yaml

helm upgrade --install wiremock bitnami/wiremock \
  --namespace appointment-booking \
  --values helm/values.yaml
```

**Helm Commands:**
```bash
# Check status
helm status wiremock -n appointment-booking

# Upgrade after config changes
helm upgrade wiremock bitnami/wiremock -n appointment-booking -f helm/values.yaml

# Uninstall
helm uninstall wiremock -n appointment-booking
```

### Option 2: Kustomize

```bash
kubectl apply -k .
```

### Option 3: Manual Kubernetes

```bash
kubectl apply -f namespace.yaml
kubectl apply -f configmap-mappings.yaml
kubectl apply -f configmap-responses.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

### Option 4: Docker Standalone

```bash
# Build the image
docker build -t capitec-wiremock:latest .

# Run the container
docker run -d -p 8089:8080 --name capitec-wiremock capitec-wiremock:latest
```

## Available Mock Endpoints

### Branch Locator
- `GET /api/v1/branches/search?q={query}` - Search branches
- `GET /api/v1/branches/country/{country}` - Get branches by country
- `GET /api/v1/branches/{branchId}/operation-hours?date={date}` - Get operation hours

### Nager Holidays
- `GET /api/v3/PublicHolidays/{year}/ZA` - Get South African public holidays

### Client Domain
- `GET /api/v1/clients/{idNumber}` - Get client by SA ID number
- `GET /api/v1/clients/9001015009087` - Specific test client (John Doe)
- `GET /api/v1/clients/0000000000000` - Returns 404 (client not found)

## Application Configuration

Configure your application to use WireMock in non-production environments:

```yaml
# application-demo.yaml or application-qa.yaml

# If using Helm deployment
branch-locator:
  base-url: http://wiremock-wiremock.appointment-booking.svc.cluster.local:8080

nager:
  api:
    base-url: http://wiremock-wiremock.appointment-booking.svc.cluster.local:8080

client-domain:
  base-url: http://wiremock-wiremock.appointment-booking.svc.cluster.local:8080

# If using manual/kustomize deployment
# branch-locator:
#   base-url: http://wiremock.appointment-booking.svc.cluster.local:8080
```

## Verifying Deployment

```bash
# Check WireMock is running
kubectl get pods -n appointment-booking -l app=wiremock

# View all mappings
kubectl exec -n appointment-booking deployment/wiremock -- wget -q -O- http://localhost:8080/__admin/mappings

# Test an endpoint
kubectl exec -n appointment-booking deployment/wiremock -- wget -q -O- "http://localhost:8080/api/v1/clients/9001015009087"
```

## Adding New Mocks

1. Add mapping file to `responses/mappings/{service}/`
2. Add response file to `responses/__files/{service}/`
3. Update `configmap-mappings.yaml` and `configmap-responses.yaml`
4. Redeploy: `kubectl apply -f configmap-*.yaml && kubectl rollout restart deployment/wiremock -n appointment-booking`

## WireMock Admin UI

Access the WireMock admin interface:
- Internal: `http://wiremock.appointment-booking.svc.cluster.local:8080/__admin`
- NodePort: Use `wiremock-admin` service for external access
