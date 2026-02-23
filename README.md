# Capitec Appointment Booking System

A comprehensive branch appointment scheduling system that enables Capitec Bank customers to pre-book branch visits for banking services, reducing wait times and improving service delivery.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Domain Contexts](#domain-contexts)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Keycloak Setup](docs/keycloak-setup.md)
- [User Flows](#user-flows)
- [Deployment](#deployment)
  - [Future Enhancements](#future-enhancements)
- [Testing](#testing)
- [Contributing](#contributing)

## 🎯 Overview

### Business Purpose

The appointment system manages the entire appointment lifecycle from initial booking through service completion, while maintaining operational flexibility for branch staff and ensuring fair access to limited branch capacity.

### Key Features

- **Customer Self-Service**: Browse available slots, book, cancel, and reschedule appointments
- **Staff Management**: Check-in customers, manage queues, mark appointments complete
- **Capacity Management**: Automatic slot generation based on branch hours and staff availability
- **Multi-Channel Notifications**: Email confirmations and reminders
- **No-Show Tracking**: Progressive restrictions for repeat no-shows
- **Multi-Branch Support**: Book at any branch across Capitec's network

### User Types

| User Type | Capabilities |
|-----------|-------------|
| **Customer** | Browse slots, book/cancel/reschedule appointments, view history |
| **Branch Staff** | Check-in customers, serve from queue, mark complete/no-show |
| **Branch Manager** | Block/unblock slots, cancel on behalf, view analytics |
| **System Admin** | Configure appointment types, manage branches, system metrics |

### Appointment Lifecycle

```
                                    ┌────────────┐
                                    │   BOOKED   │
                                    └─────┬──────┘
                                          │
            ┌─────────────┬───────────────┼───────────────┬─────────────┐
            │             │               │               │             │
            ▼             ▼               ▼               ▼             ▼
     ┌────────────┐┌────────────┐ ┌────────────┐ ┌────────────┐┌────────────┐
     │ RESCHEDULED││ CANCELLED  │ │ CHECKED_IN │ │  NO_SHOW   ││ EXPIRED    │
     └─────┬──────┘└────────────┘ └─────┬──────┘ └────────────┘└────────────┘
           │                            │
           │                            ▼
           │                     ┌────────────┐
           │                     │IN_PROGRESS │
           │                     └─────┬──────┘
           │                           │
           ▼                           ▼
     ┌────────────┐              ┌────────────┐
     │   BOOKED   │              │ COMPLETED  │
     │  (new slot)│              └────────────┘
     └────────────┘
```

**Status Descriptions:**
- **BOOKED** - Appointment confirmed, awaiting customer arrival
- **RESCHEDULED** - Moved to new slot (creates new BOOKED appointment)
- **CANCELLED** - Cancelled by customer or staff
- **CHECKED_IN** - Customer arrived at branch
- **IN_PROGRESS** - Staff serving customer
- **COMPLETED** - Service finished
- **NO_SHOW** - Customer didn't arrive
- **EXPIRED** - Customer arrived after slot start time + grace period passed

## 🏗️ Architecture

The application follows **Domain-Driven Design (DDD)** principles with clearly separated bounded contexts:

```
                                ┌─────────────────────┐
                                │     /api/v1/*       │
                                │   (REST Endpoints)  │
                                └─────────────────────┘
                                          │
    ┌───────────┬───────────┬─────────────┼─────────────┬───────────┬───────────┐
    │           │           │             │             │           │           │
    ▼           ▼           ▼             ▼             ▼           ▼           ▼
┌────────┐ ┌────────┐ ┌────────┐    ┌────────┐    ┌────────┐ ┌────────┐ ┌────────┐
│Appoint-│ │  User  │ │  Auth  │    │ Branch │    │  Slot  │ │ Staff  │ │Location│
│  ment  │ │Context │ │Context │    │Context │    │Context │ │Context │ │Context │
└────────┘ └────────┘ └───┬────┘    └───┬────┘    └───┬────┘ └───┬────┘ └───┬────┘
                          │             │             │          │          │
                          ▼             ▼             ▼          ▼          ▼
                    ┌──────────┐  ┌──────────┐  ┌──────────┐┌──────────┐┌──────────┐
                    │ Keycloak │  │  Staff   │  │  Nager   ││  Client  ││  Branch  │
                    │(External)│  │ Schedule │  │   API    ││  Domain  ││  Locator │
                    └──────────┘  │ Context  │  │(External)││(External)││(External)│
                                  └──────────┘  └──────────┘└──────────┘└──────────┘

                                ┌─────────────────────┐
                                │    Shared Kernel    │
                                │ (Day/Holiday Utils) │
                                └─────────────────────┘
```

**Diagram Explanation:**
- **Arrow (▼)** = "calls" or "depends on"
- **REST Endpoints** call into any bounded context
- **Bounded Contexts** are independent modules (siblings, not hierarchical)
- **External Services** are called by specific contexts via adapters
- **Shared Kernel** contains common utilities used by all contexts

### External Service Dependencies

| Service | Purpose | Mock Available |
|---------|---------|----------------|
| **Branch Locator** | Branch information and operation hours | ✅ WireMock |
| **Nager API** | South African public holidays | ✅ WireMock |
| **Client Domain** | Capitec client lookup by ID | ✅ WireMock |
| **Keycloak** | Authentication & authorization | ✅ Testcontainers |

## 🛠️ Tech Stack

### Core Framework
- **Java 25** with preview features
- **Spring Boot 3.5.7**
- **Spring Security** with OAuth2/OIDC
- **Spring Data JDBC**

### Database & Caching
- **PostgreSQL** - Primary database
- **Liquibase** - Database migrations
- **Caffeine** - In-memory caching

### Messaging & Events
- **Apache Kafka** - Event streaming
- **Spring Mail** - Email notifications

### Kafka Event Strategy

The application uses Apache Kafka for reliable event-driven communication. This section documents the current strategy and planned improvements.

#### Event Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PRODUCER SIDE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  App → KafkaEventPublisher → Kafka                                          │
│              │                  │                                           │
│              │ (on failure)     │ (success)                                 │
│              ▼                  ▼                                           │
│        DeadLetterImpl      Message delivered                                │
│              │                                                              │
│              ▼                                                              │
│         Database                                                            │
│    (user_dead_letter_event)                                                 │
│              │                                                              │
│              ▼                                                              │
│  RetryEventPublisherScheduler                                               │
│         (re-publish)                                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           CONSUMER SIDE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Kafka → @KafkaListener → Business Logic                                    │
│              │                   │                                          │
│              │                   │ (on failure)                             │
│              │                   ▼                                          │
│              │           DefaultErrorHandler                                │
│              │                   │                                          │
│              │                   │ (retries exhausted)                      │
│              │                   ▼                                          │
│              │      DeadLetterPublishingRecoverer                           │
│              │                   │                                          │
│              │        ┌──────────┴──────────┐                               │
│              │        │                     │                               │
│              │   isRetryable?          !isRetryable                         │
│              │        │                     │                               │
│              │        ▼                     ▼                               │
│              │   topic.retry            topic.DLT                           │
│              │        │                     │                               │
│              │        ▼                     ▼                               │
│              │  Recovery Listener      Dead Letter                          │
│              │   (reprocesses)         (investigation)                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Error Handling Strategy

| Scenario | Strategy | Outcome |
|----------|----------|---------|
| **Producer fails to send** | Save to DB via `DeadLetterImpl` | Scheduler retries later |
| **Consumer processing fails (retryable)** | Retry with backoff → `.retry` topic | Recovery listener reprocesses |
| **Consumer processing fails (non-retryable)** | Send to `.DLT` topic | Manual investigation |
| **DLT publish fails** | Fail fast, don't acknowledge | Consumer retries same message |

#### Exception Classification

**Retryable Exceptions** (will be retried):
- `SocketException`, `ConnectException`, `TimeoutException`
- `NotEnoughReplicasException`, `TransactionTimedOutException`
- `MailSenderException` (email server temporarily down)

**Non-Retryable Exceptions** (go directly to DLT):
- `DeserializationException`, `ClassCastException`
- `NullPointerException`, `IllegalArgumentException`
- `ValidateException`

#### Topic Naming Convention

| Original Topic | Retry Topic | Dead Letter Topic |
|----------------|-------------|-------------------|
| `registration-event` | `registration-event.retry` | `registration-event.DLT` |
| `appointment-booked` | `appointment-booked.retry` | `appointment-booked.DLT` |

#### Polymorphic Serialization

Events use Jackson `@JsonTypeInfo` annotations for polymorphic serialization:

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OriginEventValue.class, name = "OriginEventValue"),
    @JsonSubTypes.Type(value = EventError.class, name = "EventError")
})
public sealed interface EventValue<K,T> { ... }
```

**Key Configuration:**
- Producer: `setAddTypeInfo(false)` - Relies on `@JsonTypeInfo` annotations
- Consumer: `setUseTypeHeaders(false)` - Reads type from JSON payload, not headers

#### Future Improvements

| Enhancement | Description | Priority |
|-------------|-------------|----------|
| **Database Fallback for Consumer DLT** | When Kafka DLT publish fails, save to database as fallback. Requires `FailureSource` enum to distinguish producer vs consumer failures. | High |
| **Consumer Dead Letter Reprocessing** | Add scheduler to reprocess consumer failures from database (different from producer retry which re-publishes). | Medium |
| **Idempotency Keys** | Add idempotency tracking to prevent duplicate processing on retry. | Medium |
| **Event Schema Registry** | Use Confluent Schema Registry for schema evolution and compatibility. | Low |
| **Distributed Tracing** | Add trace IDs to Kafka headers for end-to-end observability. | Medium |

**Proposed Database Fallback Flow:**
```
Consumer DLT publish fails
         │
         ▼
Save to DB with failureSource=CONSUMER
         │
         ▼
Consumer Retry Scheduler (separate from producer scheduler)
         │
         ▼
Reprocess (not re-publish) the event
```

### Authentication
- **Keycloak 26.0.5** - Identity and access management
- **OAuth2 Resource Server** - JWT validation

### Resilience
- **Resilience4j** - Circuit breaker and retry patterns

### Build & Test
- **Gradle 9.2** - Build tool
- **JUnit 5** - Testing framework
- **Testcontainers** - Integration testing
- **WireMock** - External service mocking

## 📦 Domain Contexts

### Appointment Context
Manages the core appointment lifecycle including booking, cancellation, rescheduling, and status transitions.

**Key Aggregates**: `Appointment`, `BookingReference`

**States**: `BOOKED` → `CHECKED_IN` → `IN_PROGRESS` → `COMPLETED` | `NO_SHOW` | `CANCELLED`

### User Context
Handles user registration, verification, password management, and account lifecycle.

**Key Aggregates**: `User`, `UserProfile`

### Authentication Context
Manages login, logout, token refresh, and admin impersonation.

**Key Services**: `AuthUseCase`, `TokenResponse`

### Branch Context
Manages branch configuration, appointment information, and operation hours overrides.

**Key Aggregates**: `Branch`, `BranchAppointmentInfo`, `OperationHoursOverride`

### Staff Context
Handles staff profiles and role management.

**Key Aggregates**: `Staff`, `StaffRole`

### Staff Schedule Context
Manages staff weekly schedules and availability.

**Key Aggregates**: `WeeklyStaffSchedule`, `DailySchedule`

### Slot Context
Handles slot generation, availability, blocking, and release.

**Key Aggregates**: `Slot`, `SlotStatus`

### Location Context
Provides branch search and geolocation features.

**Key Services**: `SearchBranchesByAreaUseCase`, `FindNearestBranchesUseCase`

### Shared Kernel (Day Context)
Shared utilities for date handling, holidays, and working day calculations.

**Key Services**: `HolidayClient`, `DayType`

## 📚 API Documentation

### API Endpoints Overview

| Context | Base Path | Description |
|---------|-----------|-------------|
| **Auth** | `/api/v1/auth` | Authentication operations |
| **User** | `/api/v1/users` | User management |
| **Customer Appointments** | `/api/v1/appointments` | Customer booking operations |
| **Staff Appointments** | `/api/v1/staff/appointments` | Staff appointment operations |
| **Branch** | `/api/v1/branches` | Branch management |
| **Staff** | `/api/v1/staff` | Staff management |
| **Staff Schedule** | `/api/v1/staff-schedules` | Schedule management |
| **Slots** | `/api/v1/slots` | Slot management |
| **Location** | `/api/v1/location-service` | Branch location search |

### OpenAPI Specifications

All API contracts are available in `src/main/resources/openapi/`:

- `auth-api.yaml` - Authentication endpoints
- `user-api.yaml` - User management endpoints
- `customer-appointment-api.yaml` - Customer appointment operations
- `staff-appointment-api.yaml` - Staff appointment operations
- `branch-api.yaml` - Branch management
- `staff-api.yaml` - Staff management
- `staff-schedule-api.yaml` - Staff schedule management
- `slot-api.yaml` - Slot management
- `location-api.yaml` - Location/branch search

### Key API Operations

#### Customer Appointments
```
POST   /api/v1/appointments/create           - Book appointment
GET    /api/v1/appointments/{id}             - Get appointment details
GET    /api/v1/appointments/customer/{user}  - Get customer's appointments
PATCH  /api/v1/appointments/{id}/cancel      - Cancel appointment
PATCH  /api/v1/appointments/{id}/reschedule  - Reschedule appointment
PATCH  /api/v1/appointments/{id}/check-in    - Self check-in
```

#### Staff Appointments
```
GET    /api/v1/staff/appointments/branches/{branchId}  - Get branch appointments
PATCH  /api/v1/staff/appointments/{id}/check-in        - Check in customer
PATCH  /api/v1/staff/appointments/{id}/start           - Start serving customer
PATCH  /api/v1/staff/appointments/{id}/complete        - Complete appointment
PATCH  /api/v1/staff/appointments/{id}/no-show         - Mark as no-show
```

#### Slot Management
```
GET    /api/v1/slots/available                    - Get available slots
POST   /api/v1/admin/slots/generate               - Generate slots (Admin)
PATCH  /api/v1/admin/slots/{id}/block             - Block slot (Admin)
PATCH  /api/v1/admin/slots/{id}/release           - Release slot (Admin)
```

## 🚀 Getting Started

### Prerequisites

- **Java 25** (with preview features enabled)
- **Docker & Docker Compose** (for local development)
- **Gradle 9.2+**


## ⚙️ Configuration

### Application Profiles

| Profile | Description |
|---------|-------------|
| `default` | Local development |
| `test` | Testing with Testcontainers |
| `demo` | Demo environment with WireMock |

### Key Configuration Properties

```yaml
# application.yaml
spring:
  application:
    name: appointment-booking

# Appointment Settings
appointment:
  cancellation:
    window-hours: 2          # Minimum hours before appointment to cancel
  booking:
    advance-hours: 1         # Minimum hours in advance to book
  no-show:
    threshold: 3             # No-shows before restriction
    restriction-days: 30     # Restriction period

# Slot Generation
slot:
  generation:
    days-ahead: 7            # Generate slots for next N days
    utilization-factor: 0.8  # Reserve 20% for walk-ins
```

## 🚢 Deployment

### Future Enhancements

The following enhancements are planned for future releases:

#### Security & Rate Limiting

| Enhancement | Description | Priority |
|-------------|-------------|----------|
| **IP-based Rate Limiting** | Add rate limiting per IP address for login endpoint to prevent distributed brute-force attacks with random credentials. Current per-email rate limiting doesn't protect against attacks using different random emails. | High |
| **CAPTCHA Integration** | Add CAPTCHA challenge after N failed login attempts from same IP to distinguish humans from bots | Medium |
| **Progressive Lockout** | Implement escalating lockout periods (5min → 15min → 30min → 1hr) instead of fixed duration | Medium |
| **Login Anomaly Detection** | Flag suspicious login patterns (unusual location, device, time) for additional verification | Low |

**Proposed IP Rate Limit Configuration:**
```yaml
rate-limit:
  login:
    max-attempts-per-email: 5      # Current: per email
    window-minutes-per-email: 300  # 5 hours
    max-attempts-per-ip: 20        # Future: per IP
    window-minutes-per-ip: 1       # 1 minute
```

#### Performance & Scalability

| Enhancement | Description | Priority |
|-------------|-------------|----------|
| **Redis Cache** | Replace Caffeine with Redis for distributed caching in multi-instance deployments | Medium |
| **Database Read Replicas** | Add read replicas for query-heavy operations | Low |

#### Infrastructure & Security

| Enhancement | Description | Priority |
|-------------|-------------|----------|
| **JAR Artifact Registry** | Set up a private artifact registry (e.g., Nexus, Artifactory, GitHub Packages) for Keycloak SPI JARs instead of storing in ConfigMaps. This enables versioning, dependency management, and cleaner CI/CD pipelines. | High |
| **HashiCorp Vault Integration** | Replace hardcoded credentials in Helm values with HashiCorp Vault for secure secret management. Use Vault Agent Injector or External Secrets Operator to inject secrets at runtime. | High |
| **Sealed Secrets** | Alternative to Vault - use Bitnami Sealed Secrets for encrypting secrets in Git | Medium |

**Proposed Vault Integration:**
```yaml
# Instead of hardcoded values in values.yaml:
# DATABASE_PASSWORD: "plaintext-password"

# Use Vault annotations:
podAnnotations:
  vault.hashicorp.com/agent-inject: "true"
  vault.hashicorp.com/role: "appointment-booking"
  vault.hashicorp.com/agent-inject-secret-db: "secret/data/appointment-booking/database"
  vault.hashicorp.com/agent-inject-template-db: |
    {{- with secret "secret/data/appointment-booking/database" -}}
    export DATABASE_PASSWORD="{{ .Data.data.password }}"
    {{- end -}}
```

**Proposed JAR Registry Setup:**
```yaml
# Keycloak deployment with JAR from registry:
initContainers:
  - name: download-spis
    image: curlimages/curl
    command:
      - sh
      - -c
      - |
        curl -o /providers/generate-username-spi.jar \
          https://registry.example.com/repository/keycloak-spis/generate-username-spi-${VERSION}.jar
        curl -o /providers/validate-credential-spi.jar \
          https://registry.example.com/repository/keycloak-spis/validate-credential-spi-${VERSION}.jar
    volumeMounts:
      - name: providers
        mountPath: /providers
```

#### Monitoring & Observability

| Enhancement | Description | Priority |
|-------------|-------------|----------|
| **Distributed Tracing** | Add OpenTelemetry for end-to-end request tracing | Medium |
| **Security Audit Logging** | Enhanced audit trail for security-sensitive operations | High |

### CI/CD Pipeline

The project uses GitHub Actions for automated build, test, and deployment.

#### Main Application Pipeline (`deploy-release.yaml`)

Triggered on push to `main` branch (excluding Keycloak modules, docs, and markdown files) or manual dispatch.

```
┌─────────────────┐
│  check-changes  │ ← Skip if only Keycloak modules/docs changed
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   build-app     │ ← Gradle build + tests
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  build-docker   │ ← Build Docker image, push to source registry
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     deploy      │ ← Push to Docker Hub
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   update-helm   │ ← Update Helm chart values.yaml with new image
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  release-notes  │ ← Create GitHub Release with notes
└─────────────────┘
```

**Features:**
- ✅ Concurrency control (no parallel deployments)
- ✅ Path filtering (skip for Keycloak modules, docs, md files)
- ✅ Automatic version stamping (date + git SHA)
- ✅ Automatic Helm chart updates
- ✅ GitHub Release creation with release notes

#### Keycloak Modules Pipeline (`build-keycloak-modules.yaml`)

Triggered on changes to Keycloak SPI module folders or manual dispatch with module selection.

```
┌─────────────────┐
│ detect-changes  │ ← Detect which module changed
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│ build  │ │ build  │ ← Build JAR, upload artifact (15 days retention)
│ gen-usr│ │ val-crd│
└───┬────┘ └───┬────┘
    │          │
    ▼          ▼
┌────────┐ ┌────────┐
│ update │ │ update │ ← Apply ConfigMap to K8s, commit metadata only
│  helm  │ │  helm  │
└───┬────┘ └───┬────┘
    │          │
    └────┬─────┘
         ▼
   ┌───────────┐
   │  summary  │ ← Build summary table
   └───────────┘
```

**Features:**
- ✅ Conditional builds (only changed modules)
- ✅ Manual trigger with module selection (all, generate-username, validate-credential)
- ✅ JAR artifacts with 15-day retention
- ✅ ConfigMap applied to K8s, JAR not committed to git (too large)

#### GitHub Actions

| Action | Location | Purpose |
|--------|----------|---------|
| `gradle-build` | `.github/actions/gradle-build/` | Build JAR with version stamping |
| `docker-build` | `.github/actions/docker-build/` | Build & push Docker image |
| `docker-deploy` | `.github/actions/docker-deploy/` | Push to Docker Hub |
| `update-helm-image` | `.github/actions/update-helm-image/` | Update app Helm chart |
| `update-keycloak-spi` | `.github/actions/update-keycloak-spi/` | Update Keycloak ConfigMap |

#### Required Secrets & Variables

**Secrets:**

| Name | Purpose |
|------|---------|
| `DOCKERHUB_USERNAME` | Docker Hub login |
| `DOCKERHUB_TOKEN` | Docker Hub access token |
| `IMAGE_ACCESS_KEY` | Source registry access |
| `IMAGE_SECRET_KEY` | Source registry secret |
| `GIT_REPO_ACCESS_TOKEN` | Push Helm changes |
| `KUBECONFIG` | K8s access (base64 encoded, optional) |

**Variables:**

| Name | Purpose |
|------|---------|
| `DOCKERHUB_REPOSITORY` | e.g., `username/appointment-booking` |
| `IMAGE_REGISTRY` | Source registry URL |
| `IMAGE_REPOSITORY` | Source repo name |
| `IMAGE_SERVER_REGION` | Source registry region |
| `ENVIRONMENT` | dev/staging/prod |
| `APP_GITHUB_EMAIL` | Git commit email |
| `APP_GITHUB_USERNAME` | Git commit username |
| `K8S_NAMESPACE` | Kubernetes namespace (default: `capitec`) |

### Docker Image

Images are published to Docker Hub:

```bash
# Pull the latest image
docker pull capitec/appointment-booking:latest

# Or pull a specific version
docker pull capitec/appointment-booking:1.0.0
```

### Local Development Options

Choose one of the following options to run the application locally:

#### Option 1: Docker Compose (Recommended for Development)

The simplest way to run locally with all dependencies:

```bash
# Start all services (PostgreSQL, Kafka, Keycloak, WireMock)
docker-compose up -d

# Run the application
./gradlew bootRun

# Or run everything including the app
docker-compose --profile app up -d
```

**docker-compose.yaml** includes:
- PostgreSQL database
- Apache Kafka (KRaft mode)
- Keycloak identity server
- WireMock for external service mocks

#### Option 2: Minikube (Recommended for Testing K8s Deployment)

For testing Kubernetes deployment locally, follow these steps in order:

---

##### Step 1: Set Up Minikube

```bash
# Start Minikube with recommended resources
# Default: 10GB memory, 6 CPUs (adjust as needed, minimum 8GB/4CPU recommended)
minikube start --memory=9735 --cpus=6

# Enable required addons
minikube addons enable ingress
minikube addons enable storage-provisioner
minikube addons enable default-storageclass

# Verify addons are enabled
minikube addons list | grep -E "ingress|storage"

# Set Kubernetes namespace to capitec
kubectl create namespace capitec
kubectl config set-context --current --namespace=capitec

# Make Docker images visible to Minikube context
# Run this in EVERY new terminal session
eval $(minikube docker-env)

# Verify Minikube is running
minikube status
kubectl get nodes
```

> **Note:** If you need to scale down resources, adjust `--memory` and `--cpus` values. Minimum recommended: `--memory=8192 --cpus=4`

---

##### Step 2: Install PostgreSQL Database

```bash
# Navigate to helm directory
cd deploy-operation/helm

# Add Bitnami Helm repository (if not already added)
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Install PostgreSQL
# IMPORTANT: Note the release name (capitec-db) - it will be used as the database host
helm upgrade --install capitec-db bitnami/postgresql \
  -n capitec \
  -f capitec-db/values.yaml \
  --set auth.username=appointment_user \
  --set auth.database=appointment_db \
  --set auth.password=your_secure_password

# Wait for database to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgresql -n capitec --timeout=120s

# Verify database is running
kubectl get pods -n capitec | grep capitec-db

# Get the database password (save this for later steps)
export POSTGRES_PASSWORD=$(kubectl get secret --namespace capitec capitec-db-postgresql -o jsonpath="{.data.password}" | base64 -d)
echo "Database Password: $POSTGRES_PASSWORD"

# Database connection details (save these):
# Host: capitec-db-postgresql.capitec.svc.cluster.local
# Port: 5432
# Database: appointment_db
# Username: appointment_user
# Password: <value from above>
```

---

##### Step 3: Install Keycloak

Keycloak uses the external PostgreSQL database installed in Step 2.

```bash
# Navigate to helm directory
cd deploy-operation/helm

# Before installing, update keycloak/values.yaml with database credentials:
# 
# externalDatabase:
#   host: "capitec-db-postgresql.capitec.svc.cluster.local"
#   port: 5432
#   user: "appointment_user"
#   database: "appointment_db"
#   password: "<POSTGRES_PASSWORD from Step 2>"
#
# extraEnvVars:
#   - name: KC_DB_URL
#     value: "jdbc:postgresql://capitec-db-postgresql.capitec.svc.cluster.local:5432/appointment_db"

# Install Keycloak
helm upgrade --install keycloak-idp bitnami/keycloak \
  -n capitec \
  -f keycloak/values.yaml \
  --set externalDatabase.host=capitec-db-postgresql.capitec.svc.cluster.local \
  --set externalDatabase.port=5432 \
  --set externalDatabase.user=appointment_user \
  --set externalDatabase.database=appointment_db \
  --set externalDatabase.password=$POSTGRES_PASSWORD

# Wait for Keycloak to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=keycloak -n capitec --timeout=300s

# Verify Keycloak is running
kubectl get pods -n capitec | grep keycloak

# Apply Keycloak SPI ConfigMaps (IMPORTANT: Do this after Keycloak is running)
kubectl apply -f generate-username-spi-configmaps.yaml -n capitec
kubectl apply -f validate-credentials-spi-configmaps.yaml -n capitec

# Verify ConfigMaps were applied and have data (data should NOT be zero)
kubectl get configmap -n capitec | grep spi
kubectl describe configmap generate-username-ui-register-spi -n capitec | head -20
kubectl describe configmap validate-credential-spi -n capitec | head -20

# Restart Keycloak to pick up the SPIs
kubectl rollout restart deployment keycloak-idp -n capitec
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=keycloak -n capitec --timeout=300s

# Get Keycloak admin password (save for appointment-server configuration)
export KEYCLOAK_ADMIN_PASSWORD=$(kubectl get secret --namespace capitec keycloak-idp -o jsonpath="{.data.admin-password}" | base64 -d)
echo "Keycloak Admin Password: $KEYCLOAK_ADMIN_PASSWORD"

# Keycloak connection details (save these):
# Host: keycloak-idp.capitec.svc.cluster.local
# Port: 8080
# Admin Username: admin (or as configured)
# Admin Password: <value from above>
# Realm: appointment-booking-DEV (create this in Keycloak UI)
```

> **📖 Keycloak Configuration Guide**
> 
> After Keycloak is running, you need to configure realms, clients, roles, and mappers.
> See the detailed setup guide: **[Keycloak Setup Guide](docs/keycloak-setup.md)**
> 
> Quick checklist:
> - [ ] Create realm `appointment-booking`
> - [ ] Create client with authentication enabled
> - [ ] Configure token exchange
> - [ ] Create roles: `app_user`, `app_staff`, `app_admin`
> - [ ] Configure role mapper for token claims
> - [ ] Set `app_user` as default role

---

##### Step 4: Install Apache Kafka

```bash
# Navigate to helm directory
cd deploy-operation/helm

# Install Kafka
# IMPORTANT: Note the release name (capitec-kafka) - used for broker connection
helm upgrade --install capitec-kafka bitnami/kafka \
  -n capitec \
  -f kafka/values.yaml

# Wait for Kafka to be ready (this may take a few minutes)
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka -n capitec --timeout=300s

# Verify Kafka pods are running
# Should see 3 controllers and 3 brokers (6 total replicas)
kubectl get pods -n capitec | grep kafka

# Kafka connection details (save these):
# Bootstrap Servers: capitec-kafka-broker-0.capitec-kafka-broker-headless:9094,capitec-kafka-broker-1.capitec-kafka-broker-headless:9094,capitec-kafka-broker-2.capitec-kafka-broker-headless:9094
```

---

##### Step 5: Install WireMock (External Service Mocks)

WireMock provides mocks for external services (Branch Locator, Nager Holidays, Client Domain).

```bash
# Navigate to helm directory
cd deploy-operation/helm

# Apply WireMock ConfigMaps (mappings and responses)
kubectl apply -f wiremock/configmap-mappings.yaml -n capitec
kubectl apply -f wiremock/configmap-responses.yaml -n capitec

# Install WireMock
helm upgrade --install wiremock bitnami/wiremock \
  -n capitec \
  -f wiremock/values.yaml

# Wait for WireMock to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=wiremock -n capitec --timeout=120s

# Verify WireMock is running
kubectl get pods -n capitec | grep wiremock

# WireMock connection details:
# Host: wiremock.capitec.svc.cluster.local
# Port: 9021

# To add custom mappings/responses, edit the ConfigMaps:
# kubectl edit configmap wiremock-mappings -n capitec
# kubectl edit configmap wiremock-responses -n capitec
# Then restart WireMock: kubectl rollout restart deployment wiremock -n capitec
```

---

##### Step 6: Install Appointment-Booking-Server

Before installing, ensure all prerequisites are running:
- ✅ PostgreSQL (Step 2)
- ✅ Keycloak (Step 3)
- ✅ Kafka (Step 4)
- ✅ WireMock (Step 5)

> **Note:** The `values.yaml` file has empty placeholders for sensitive data. You must fill in all required values before deployment.

```bash
# Navigate to helm directory
cd deploy-operation/helm

# Verify all dependencies are running
kubectl get pods -n capitec

# IMPORTANT: Update appointment-booking-server/values.yaml with your credentials
# All sensitive values are empty placeholders that need to be configured:
#
# baseEnv:
#   # Database (from Step 2)
#   - name: DATABASE_HOST
#     value: "capitec-db-postgresql"  # Your PostgreSQL release name
#   - name: DATABASE_DATABASE
#     value: "appointment_db"
#   - name: DATABASE_USERNAME
#     value: "appointment_user"
#   - name: DATABASE_PASSWORD
#     value: "<POSTGRES_PASSWORD from Step 2>"
#
#   # Keycloak (from Step 3)
#   - name: AUTH_SERVER_TOKEN_URL
#     value: "http://keycloak-idp.capitec.svc.cluster.local:8080/realms/<YOUR_REALM>/protocol/openid-connect/certs"
#   - name: ISSUER_URI
#     value: "http://keycloak-idp.capitec.svc.cluster.local:8080/realms/<YOUR_REALM>"
#   - name: KEYCLOAK_REALM
#     value: "<YOUR_REALM>"
#   - name: KEYCLOAK_DOMAIN
#     value: "keycloak-idp.capitec.svc.cluster.local:8080"
#   - name: KEYCLOAK_AUTH_URL
#     value: "http://keycloak-idp.capitec.svc.cluster.local:8080"
#   - name: KEYCLOAK_ADMIN_CLIENT_ID
#     value: "<YOUR_CLIENT_ID>"
#   - name: KEYCLOAK_USERNAME
#     value: "admin"
#   - name: KEYCLOAK_PASSWORD
#     value: "<KEYCLOAK_ADMIN_PASSWORD from Step 3>"
#
#   # Kafka (from Step 4)
#   - name: BROKER_BOOTSTRAP_SERVERS
#     value: "capitec-kafka-broker-0.capitec-kafka-broker-headless:9094,capitec-kafka-broker-1.capitec-kafka-broker-headless:9094,capitec-kafka-broker-2.capitec-kafka-broker-headless:9094"
#
#   # External Services
#   - name: CLIENT_DOMAIN_BASE_URL
#     value: "http://wiremock:9021"
#   - name: HOLIDAYS_CLIENT_API
#     value: "https://date.nager.at"
#   - name: CAPITEC_BRANCH_LOCATOR_API
#     value: "http://wiremock:9021/api/v1/branches"
#
#   # Email (configure your SMTP server)
#   - name: MAIL_HOST
#     value: "<YOUR_SMTP_HOST>"
#   - name: MAIL_PORT
#     value: "<YOUR_SMTP_PORT>"
#   - name: MAIL_USERNAME
#     value: "<YOUR_SMTP_USERNAME>"
#   - name: MAIL_PASSWORD
#     value: "<YOUR_SMTP_PASSWORD>"
#
#   # CORS (update for your frontend URLs)
#   - name: CORS_ALLOWED_ORIGIN
#     value: "http://localhost:3000,http://your-frontend-url"
#
# ingress:
#   hosts:
#     - host: "<YOUR_INGRESS_HOST>"  # e.g., appointment.your-domain.com

# Install Appointment-Booking-Server
helm upgrade --install appointment-booking-server ./appointment-booking-server \
  -n capitec \
  -f appointment-booking-server/values.yaml \
  --set baseEnv[0].value="$POSTGRES_PASSWORD" \
  --set baseEnv[1].value="$KEYCLOAK_ADMIN_PASSWORD"

# Wait for application to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=appointment-booking-server -n capitec --timeout=300s

# Verify application is running
kubectl get pods -n capitec | grep appointment-booking

# Check application logs
kubectl logs -f deployment/appointment-booking-server -n capitec

# Access the application
minikube service appointment-booking-server -n capitec --url
```

---

##### Step 7: Install Appointment-Booking-Web-Client (Optional)

> **Note:** The web client has its own separate repository at:
> **https://github.com/tswlun002/appointment-booking-web-client.git**

You have two options to get the Docker image:

**Option A: Pull from Docker Hub (Recommended)**

```bash
# Pull the latest web client image from Docker Hub
docker pull capitec/appointment-booking-web-client:latest
```

**Option B: Build Locally from Source**

```bash
# Clone the web client repository
git clone https://github.com/tswlun002/appointment-booking-web-client.git
cd appointment-booking-web-client

# Make Minikube see local Docker images
eval $(minikube docker-env)

# Build the Docker image locally
docker build -t appointment-booking-web-client:latest .

# Verify the image is available
docker images | grep appointment-booking-web-client
```

**Install the Web Client:**

```bash
# Navigate to helm directory
cd deploy-operation/helm

# IMPORTANT: Update appointment-booking-web-client/values.yaml with your configuration
# All sensitive values are empty placeholders that need to be configured:
#
# baseEnv:
#   - name: VITE_API_BASE_URL
#     value: "http://<YOUR_SERVER_INGRESS_HOST>"  # URL to appointment-booking-server
#   - name: VITE_INTERNAL_BASE_URL
#     value: "http://<YOUR_SERVER_INGRESS_HOST>"
#   - name: VITE_REALM
#     value: "<YOUR_KEYCLOAK_REALM>"
#
# ingress:
#   hosts:
#     - host: "<YOUR_CLIENT_INGRESS_HOST>"  # e.g., appointment.your-domain.com

# Install the web client
helm upgrade --install appointment-booking-web-client ./appointment-booking-web-client \
  -n capitec \
  -f appointment-booking-web-client/values.yaml

# Wait for web client to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=appointment-booking-web-client -n capitec --timeout=120s

# Verify web client is running
kubectl get pods -n capitec | grep web-client

# Access the web client
minikube service appointment-booking-web-client -n capitec --url
```

---

##### Quick Reference: All Connection Details

| Service | Host | Port | Notes |
|---------|------|------|-------|
| PostgreSQL | `capitec-db-postgresql.capitec.svc.cluster.local` | 5432 | Database: `appointment_db` |
| Keycloak | `keycloak-idp.capitec.svc.cluster.local` | 8080 | Realm: `<YOUR_REALM>` |
| Kafka | `capitec-kafka-broker-headless` | 9094 | 3 brokers |
| WireMock | `wiremock.capitec.svc.cluster.local` | 9021 | External service mocks |
| App Server | `appointment-booking-server.capitec.svc.cluster.local` | 8083 | Main application |
| Web Client | `appointment-booking-web-client.capitec.svc.cluster.local` | 3000 | Frontend (separate repo) |

---

##### Troubleshooting

```bash
# Check pod status
kubectl get pods -n capitec

# View logs for a specific pod
kubectl logs -f <pod-name> -n capitec

# Describe pod for events/errors
kubectl describe pod <pod-name> -n capitec

# Check services
kubectl get svc -n capitec

# Check ingress
kubectl get ingress -n capitec

# Restart a deployment
kubectl rollout restart deployment <deployment-name> -n capitec

# Delete and recreate Minikube (fresh start)
minikube delete
minikube start --memory=9735 --cpus=6

# Port forward to access a service directly
kubectl port-forward svc/appointment-booking-server 8083:8083 -n capitec
kubectl port-forward svc/keycloak-idp 8080:8080 -n capitec
```

---

##### Clean Up

```bash
# Uninstall all Helm releases
helm uninstall appointment-booking-web-client -n capitec
helm uninstall appointment-booking-server -n capitec
helm uninstall wiremock -n capitec
helm uninstall capitec-kafka -n capitec
helm uninstall keycloak-idp -n capitec
helm uninstall capitec-db -n capitec

# Delete namespace (removes everything)
kubectl delete namespace capitec

# Stop Minikube
minikube stop

# Delete Minikube cluster
minikube delete
```

**Requirements for Minikube:**
- Minikube installed (`brew install minikube` or [download](https://minikube.sigs.k8s.io/docs/start/))
- kubectl installed
- Helm installed
- Minimum 10GB RAM and 6 CPUs allocated to Minikube (can scale down to 8GB/4CPU)

### Kubernetes Deployment

The application is designed for Kubernetes deployment. See `deploy-operation/` for:

- **WireMock** - External service mocks for demo/QA environments

#### WireMock Setup (Demo/QA)

For environments without access to external services:

```bash
# Using Helm (Bitnami)
helm repo add bitnami https://charts.bitnami.com/bitnami
kubectl apply -f deploy-operation/wiremock/configmap-mappings.yaml
kubectl apply -f deploy-operation/wiremock/configmap-responses.yaml
helm upgrade --install wiremock bitnami/wiremock -n appointment-booking -f deploy-operation/wiremock/helm/values.yaml
```

See `deploy-operation/wiremock/README.md` for detailed setup.

#### Mocked External Services

| Service | Endpoint | Description |
|---------|----------|-------------|
| Branch Locator | `GET /api/v1/branches/search` | Search branches |
| Branch Locator | `GET /api/v1/branches/country/{country}` | Get branches by country |
| Nager Holidays | `GET /api/v3/PublicHolidays/{year}/ZA` | SA public holidays |
| Client Domain | `GET /api/v1/clients/{idNumber}` | Get client by ID |

### Helm Chart

The Helm chart always deploys the latest image from Docker Hub:

```bash
# Add the Helm repository (if hosted)
helm repo add capitec https://capitec.github.io/helm-charts

# Install the application
helm upgrade --install appointment-booking capitec/appointment-booking \
  --namespace appointment-booking \
  --create-namespace \
  --set image.tag=latest
```

Or deploy with a specific version:

```bash
helm upgrade --install appointment-booking capitec/appointment-booking \
  --namespace appointment-booking \
  --set image.tag=1.0.0
```

### Keycloak Ingress & Network Policy

Keycloak is configured with ingress and network policy for secure access:

#### Ingress Configuration
- **Enabled**: Yes
- **Ingress Class**: nginx
- **Path Type**: Prefix
- **Hostname**: `keycloak.local` (configurable)

#### Network Policy
Only the following pods can access Keycloak:
- **appointment-booking-server**: Internal service communication
- **ingress-nginx**: External access via ingress controller

This ensures Keycloak is not directly accessible from other pods in the cluster.

#### Server Connection to Keycloak

The appointment-booking-server connects to Keycloak using the internal Kubernetes service URL:

```yaml
# In appointment-booking-server values.yaml
baseEnv:
  - name: KEYCLOAK_DOMAIN
    value: "http://keycloak.<namespace>.svc.cluster.local:8080"
  - name: AUTH_SERVER_TOKEN_URL
    value: "http://keycloak.<namespace>.svc.cluster.local:8080/realms/<realm>/protocol/openid-connect/token"
  - name: ISSUER_URI
    value: "http://keycloak.<namespace>.svc.cluster.local:8080/realms/<realm>"
  - name: KEYCLOAK_AUTH_URL
    value: "http://keycloak.<namespace>.svc.cluster.local:8080"
```

Replace `<namespace>` with your deployment namespace (e.g., `default`, `appointment-booking`) and `<realm>` with your Keycloak realm name.

## 👥 User Flows

The system supports three user profiles with distinct workflows:

### Customer (app_user)
- Browse available appointment slots
- Book appointments for banking services
- View, reschedule, or cancel their appointments
- Check-in upon arrival at the branch

### Staff/Consultant (app_staff)
- View branch appointment queue
- Check-in customers
- Start serving appointments
- Mark appointments as complete
- Cancel appointments with reason

### Branch Manager/Admin (app_admin)
- All staff capabilities
- Manage branch information and hours
- Add/remove staff members
- Configure branch capacity and services
- Override branch hours for holidays

> **📖 Complete User Flow Documentation**
> 
> For detailed step-by-step flows with API examples, see: **[Keycloak Setup & User Flows](docs/keycloak-setup.md#part-2-user-flows--api-usage)**

---

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

### Test Infrastructure

Tests use **Testcontainers** for:
- PostgreSQL database
- Kafka broker
- Keycloak identity server
- WireMock for external APIs

### Integration Test Example

```java
@SpringBootTest
@Testcontainers
class AppointmentBookingIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));
    
    @Test
    void shouldBookAppointment() {
        // Test implementation
    }
}
```

## 📁 Project Structure

```
appointment-booking/
├── .github/
│   ├── actions/
│   │   ├── docker-build/      # Docker build action
│   │   └── gradle-build/      # Gradle build action
│   └── workflows/
│       └── deploy-release.yaml
├── deploy-operation/
│   └── wiremock/              # WireMock setup for demo/QA
│       ├── helm/              # Helm deployment
│       ├── responses/         # Mock response files
│       └── *.yaml             # K8s manifests
├── src/
│   ├── main/
│   │   ├── java/capitec/branch/appointment/
│   │   │   ├── appointment/   # Appointment context
│   │   │   ├── authentication/# Auth context
│   │   │   ├── branch/        # Branch context
│   │   │   ├── location/      # Location context
│   │   │   ├── slot/          # Slot context
│   │   │   ├── staff/         # Staff context
│   │   │   ├── staffschedule/ # Staff schedule context
│   │   │   ├── user/          # User context
│   │   │   └── utils/         # Shared kernel
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── db/migrations/ # Liquibase migrations
│   │       └── openapi/       # API contracts
│   └── test/
├── generate-username-ui-register-module/  # Keycloak SPI
├── validate-credential-module/            # Keycloak SPI
├── build.gradle.kts
├── Dockerfile
└── README.md
```

## 🔐 Keycloak SPIs

The project includes two Keycloak Service Provider Interfaces (SPIs):

### generate-username-ui-register-module
Generates unique usernames during user registration in Keycloak.

### validate-credential-module
Custom credential validation for the Capitec authentication flow.

Build SPIs:
```bash
./gradlew :generate-username-ui-register-module:build
./gradlew :validate-credential-module:build
```

## 🤝 Contributing

1. Create a feature branch from `main`
2. Make your changes following DDD principles
3. Ensure all tests pass
4. Submit a pull request

### Code Style

- Follow Java naming conventions
- Use meaningful names for classes, methods, and variables
- Keep bounded contexts isolated
- Write unit tests for domain logic
- Write integration tests for use cases

### Branch Naming

- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Production hotfixes
- `refactor/` - Code refactoring

## 📄 License

Copyright © 2026 Capitec Bank. All rights reserved.

---

## 📞 Support

For questions or issues, contact the Branch Appointment Team.
