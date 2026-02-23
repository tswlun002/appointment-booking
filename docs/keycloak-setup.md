# Keycloak Setup Guide

This guide provides detailed instructions for setting up Keycloak for the Appointment Booking System, including realm creation, client configuration, role setup, and token exchange configuration.

## Table of Contents

- [1. Initial Setup](#1-initial-setup)
  - [1.1 Create Realm](#11-create-realm)
  - [1.2 Create Client](#12-create-client)
  - [1.3 Configure Token Exchange](#13-configure-token-exchange)
- [2. Role Configuration](#2-role-configuration)
  - [2.1 Create Client Roles](#21-create-client-roles)
  - [2.2 Configure Role Mapper](#22-configure-role-mapper)
  - [2.3 Set Default Roles](#23-set-default-roles)
- [3. Realm Settings](#3-realm-settings)
- [4. User Flows](#4-user-flows)
- [5. Troubleshooting](#5-troubleshooting)

---

## 1. Initial Setup

### 1.1 Create Realm

1. **Login to Keycloak Admin Console**
   - Navigate to Keycloak admin URL (e.g., `http://localhost:8080/admin`)
   - Login with admin credentials

2. **Create a New Realm**
   - Click the dropdown next to the current realm name (top-left)
   - Click **"Create Realm"**
   - Enter realm name: `appointment-booking`
   - Click **"Create"**

> **Note:** Ensure the newly created realm is selected (displayed in the top-left dropdown) before proceeding.

### 1.2 Create Client

1. **Navigate to Clients**
   - In the left sidebar, click **"Clients"**
   - Click **"Create client"**

2. **General Settings**
   - **Client type:** OpenID Connect
   - **Client ID:** `appointment-booking-client` (or your preferred name)
   - Click **"Next"**

3. **Capability Config**
   - Toggle **ON**: `Client authentication`
   - Check **"Standard flow"** ✓
   - Check **"Direct access grants"** ✓
   - Check **"Service accounts roles"** ✓
   - Click **"Next"**

4. **Login Settings**
   - **Valid redirect URIs:** Add your application URLs
     - `http://localhost:3000/*` (development)
     - `https://your-app-domain.com/*` (production)
   - **Web origins:** Add origins for CORS
     - `http://localhost:3000`
     - `https://your-app-domain.com`
   - Click **"Save"**

### 1.3 Configure Token Exchange

Token exchange allows the application to exchange tokens for different users or obtain tokens with different permissions.

1. **Navigate to Client Settings**
   - Go to **Clients** → Select your client
   - Click the **"Advanced"** tab

2. **Configure Token Exchange Settings**
   - Scroll to **"OpenID Connect Compatibility Modes"** or **"Fine Grain OpenID Connect Configuration"**
   - Toggle **ON**: `Use refresh tokens`
   - Set **"Allow refresh token in Standard Token exchange"** to: `Same session`
   - Click **"Save"**

3. **Get Client Secret**
   - Click the **"Credentials"** tab
   - Copy the **Client secret** value
   - Store securely for application configuration

---

## 2. Role Configuration

### 2.1 Create Client Roles

The system uses three primary roles:

| Role | Description | Typical User |
|------|-------------|--------------|
| `app_user` | Basic authenticated user role | All users |
| `app_staff` | Branch staff/consultant role | Consultants serving customers |
| `app_admin` | Branch manager/admin role | Managers configuring branches |

**Steps to Create Roles:**

1. **Navigate to Client Roles**
   - Go to **Clients** → Select your client
   - Click the **"Roles"** tab
   - Click **"Create role"**

2. **Create Each Role:**

   **Role 1: app_user**
   - Role name: `app_user`
   - Description: `Basic user role for all authenticated customers`
   - Click **"Save"**

   **Role 2: app_staff**
   - Role name: `app_staff`
   - Description: `Staff role for branch consultants`
   - Click **"Save"**

   **Role 3: app_admin**
   - Role name: `app_admin`
   - Description: `Admin role for branch managers`
   - Click **"Save"**

### 2.2 Configure Role Mapper

To include roles in the access token claims, configure a mapper:

1. **Navigate to Client Scopes**
   - Go to **Clients** → Select your client
   - Click the **"Client scopes"** tab
   - Click `<your-client-id>-dedicated`

2. **Add Predefined Mapper**
   - Click **"Add mapper"** → **"From predefined mappers"**
   - Select **"client roles"**
   - Click **"Add"**

3. **Create Custom Role Mapper**
   - Click **"Add mapper"** → **"By configuration"**
   - Select **"User Client Role"**
   - Configure:
     - **Name:** `customer-role-mapper`
     - **Client ID:** Select your client
     - **Multivalued:** Toggle **ON**
     - **Token Claim Name:** `roles`
     - **Claim JSON Type:** `String`
     - **Add to ID token:** Toggle **ON** (optional)
     - **Add to access token:** Toggle **ON**
     - **Add to userinfo:** Toggle **ON** (optional)
     - **Add to token introspection:** Toggle **ON** (optional)
   - Click **"Save"**

### 2.3 Set Default Roles

Configure the default role assigned to new users upon registration:

1. **Navigate to Realm Settings**
   - Click **"Realm settings"** in the left sidebar
   - Click the **"User registration"** tab (or **"User profile"** depending on version)

2. **Assign Default Role**
   - Click the **"Default roles"** tab or **"Roles"** → **"Default roles"**
   - Click **"Assign role"**
   - Filter by: Client roles → Select your client
   - Select `app_user`
   - Click **"Assign"**

> **Result:** Every newly registered user automatically receives the `app_user` role.

---

## 3. Realm Settings

### Enable Username Editing

Allow users to change their username if needed:

1. Go to **"Realm settings"**
2. Click the **"Login"** tab
3. Toggle **ON**: `Edit username`
4. Click **"Save"**

### Configure Email Settings (Optional)

For email verification and notifications:

1. Go to **"Realm settings"**
2. Click the **"Email"** tab
3. Configure SMTP settings:
   - **From:** `noreply@your-domain.com`
   - **Host:** Your SMTP server
   - **Port:** 587 (or as required)
   - Enable **Authentication** if required
4. Click **"Save"**

---

## 4. User Flows

For detailed user flows covering customer, staff, and admin journeys, see the dedicated documentation:

📄 **[User Flows Documentation](./user-flows.md)**

This includes:
- **Customer Flow (app_user):** Registration, login, booking appointments, managing appointments, check-in
- **Staff/Consultant Flow (app_staff):** Viewing branch queue, serving customers, managing availability
- **Admin/Branch Manager Flow (app_admin):** Branch management, appointment configuration, staff management, slot management

---

## 5. Troubleshooting

### Common Issues

#### Token does not contain roles

**Problem:** The `roles` claim is missing from the access token.

**Solution:**
1. Verify the role mapper is configured correctly (Section 2.2)
2. Ensure "Add to access token" is enabled
3. Check that the user has roles assigned
4. Regenerate tokens after configuration changes

#### User registration fails

**Problem:** New users cannot register.

**Solution:**
1. Ensure email server is configured (if email verification is enabled)
2. Check that the `app_user` default role is assigned (Section 2.3)
3. Verify the client has proper redirect URIs configured

#### Token exchange fails

**Problem:** Unable to exchange tokens.

**Solution:**
1. Verify token exchange is enabled on the client (Section 1.3)
2. Check that "Use refresh tokens" is enabled
3. Ensure the client has `service accounts roles` capability

#### CORS errors

**Problem:** Browser blocks requests due to CORS.

**Solution:**
1. Add your frontend origin to "Web origins" in client settings
2. Ensure proper redirect URIs are configured

---

## Quick Reference

### Keycloak Endpoints

| Purpose | Endpoint |
|---------|----------|
| Token | `POST /realms/{realm}/protocol/openid-connect/token` |
| Userinfo | `GET /realms/{realm}/protocol/openid-connect/userinfo` |
| Logout | `POST /realms/{realm}/protocol/openid-connect/logout` |
| Admin | `/admin/realms/{realm}` |

### Role Hierarchy

```
app_admin (Branch Manager)
    ├── All app_staff permissions
    ├── Branch configuration
    ├── Staff management
    └── Slot blocking/unblocking

app_staff (Consultant)
    ├── All app_user permissions
    ├── View branch queue
    ├── Start/complete appointments
    └── Mark no-shows

app_user (Customer)
    ├── View available slots
    ├── Book appointments
    ├── Cancel/reschedule own appointments
    └── Check-in
```

### API Role Requirements

| Endpoint | Required Role |
|----------|---------------|
| `/api/v1/appointments/*` | `app_user` |
| `/api/v1/staff/appointments/*` | `app_staff` |
| `/api/v1/branches/*` | `app_admin` |
| `/api/v1/staff/*` (POST, PATCH) | `app_admin` |
| `/api/v1/slots/*/block` | `app_admin` |
| `/api/v1/auth/logout` | `app_user` |

---

## Next Steps

After completing Keycloak setup:

1. ✅ Configure application `application.yaml` with Keycloak settings
2. ✅ Test authentication flow with a test user
3. ✅ Create admin user and assign `app_admin` role
4. ✅ Create staff users and assign `app_staff` role
5. ✅ Configure branches and appointment info
6. ✅ Generate initial slots

---

*Last updated: February 2026*
