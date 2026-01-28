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
- [Deployment](#deployment)
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

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/capitec/appointment-booking.git
   cd appointment-booking
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**
   - API: `http://localhost:8080`
   - Keycloak: `http://localhost:8180`

### Environment Variables

Create a `.env` file in the project root:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointment_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Keycloak
KEYCLOAK_AUTH_SERVER_URL=http://localhost:8180
KEYCLOAK_REALM=capitec
KEYCLOAK_CLIENT_ID=appointment-service
KEYCLOAK_CLIENT_SECRET=your-client-secret

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# External Services
BRANCH_LOCATOR_BASE_URL=http://localhost:8089
NAGER_API_BASE_URL=http://localhost:8089
CLIENT_DOMAIN_BASE_URL=http://localhost:8089
```

## ⚙️ Configuration

### Application Profiles

| Profile | Description |
|---------|-------------|
| `default` | Local development |
| `test` | Testing with Testcontainers |
| `demo` | Demo environment with WireMock |
| `qa` | QA environment |
| `prod` | Production |

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

### CI/CD Pipeline

The project uses GitHub Actions for CI/CD:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Build App  │ ──▶ │ Build Docker│ ──▶ │  Release    │
│  (Gradle)   │     │   Image     │     │   Notes     │
└─────────────┘     └─────────────┘     └─────────────┘
                          │
                          ▼
                    ┌─────────────┐
                    │  Docker Hub │
                    │   (Public)  │
                    └─────────────┘
```

**Workflow**: `.github/workflows/deploy-release.yaml`

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

For testing Kubernetes deployment locally:

```bash
# 1. Start Minikube
minikube start --memory=4096 --cpus=2

# 2. Enable ingress addon
minikube addons enable ingress

# 3. Create namespace
kubectl create namespace appointment-booking

# 4. Deploy WireMock (for external service mocks)
kubectl apply -f deploy-operation/wiremock/configmap-mappings.yaml
kubectl apply -f deploy-operation/wiremock/configmap-responses.yaml
helm repo add bitnami https://charts.bitnami.com/bitnami
helm upgrade --install wiremock bitnami/wiremock -n appointment-booking -f deploy-operation/wiremock/helm/values.yaml

# 5. Deploy the application (uses latest image from Docker Hub)
kubectl apply -f deploy-operation/k8s/

# 6. Access the application
minikube service appointment-booking -n appointment-booking
```

**Requirements for Minikube:**
- Minikube installed (`brew install minikube` or [download](https://minikube.sigs.k8s.io/docs/start/))
- kubectl installed
- Helm installed
- Minimum 4GB RAM allocated to Minikube

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
