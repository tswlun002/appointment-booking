# User Flows - Appointment Booking System

This document describes the user journeys for each role in the Appointment Booking System.

## Table of Contents

- [Overview](#overview)
- [1. Customer Flow (app_user)](#1-customer-flow-app_user)
  - [1.1 Registration](#11-registration)
  - [1.2 Login](#12-login)
  - [1.3 Password Reset](#13-password-reset)
  - [1.4 Browse and Book Appointment](#14-browse-and-book-appointment)
  - [1.5 Manage Appointments](#15-manage-appointments)
  - [1.6 Check-in](#16-check-in)
- [2. Staff/Consultant Flow (app_staff)](#2-staffconsultant-flow-app_staff)
  - [2.1 View Branch Queue](#21-view-branch-queue)
  - [2.2 Serve Customer](#22-serve-customer)
  - [2.3 Staff Availability](#23-staff-availability)
- [3. Admin/Branch Manager Flow (app_admin)](#3-adminbranch-manager-flow-app_admin)
  - [3.1 Branch Management](#31-branch-management)
  - [3.2 Configure Branch Appointments](#32-configure-branch-appointments)
  - [3.3 Override Operation Hours](#33-override-operation-hours)
  - [3.4 Staff Management](#34-staff-management)
  - [3.5 Slot Management](#35-slot-management)
- [API Role Requirements](#api-role-requirements)

---

## Overview

The system supports three user profiles:

| Role | Description | Typical User |
|------|-------------|--------------|
| `app_user` | Basic authenticated user role | Customers booking appointments |
| `app_staff` | Branch staff/consultant role | Consultants serving customers |
| `app_admin` | Branch manager/admin role | Managers configuring branches |

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

---

## 1. Customer Flow (app_user)

The customer flow covers the complete journey from registration to appointment completion.

### 1.1 Registration

```
┌────────────────────────────────────────────────────────────────┐
│                    CUSTOMER REGISTRATION                       │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Customer visits application                                │
│     └─► GET /api/v1/users/auth/generate/username               │
│         (Generates unique username)                            │
│                                                                 │
│  2. Customer submits registration form                         │
│     └─► POST /api/v1/users/auth/register                       │
│         Body: { email, password, firstName, lastName, ...}     │
│         Response: "Verification code sent"                     │
│                                                                 │
│  3. Customer receives OTP via email                            │
│                                                                 │
│  4. Customer verifies OTP                                      │
│     └─► PUT /api/v1/users/auth/verify                          │
│         Body: { email, otp, isCapitecClient }                  │
│         Response: Auto-login with tokens (cookie)              │
│                                                                 │
│  ✓ User is now registered with 'app_user' role                │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Register:**
```bash
POST /api/v1/users/auth/register
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+27123456789"
}
```

**Example Request - Verify OTP:**
```bash
PUT /api/v1/users/auth/verify
Content-Type: application/json

{
  "email": "customer@example.com",
  "otp": "123456",
  "isCapitecClient": true
}
```

### 1.2 Login

```
┌────────────────────────────────────────────────────────────────┐
│                      CUSTOMER LOGIN                            │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Customer submits credentials                               │
│     └─► POST /api/v1/auth/login                                │
│         Body: { username, password }                           │
│         Response: Access token + Refresh token (cookie)        │
│                                                                 │
│  2. Token refresh (when access token expires)                  │
│     └─► POST /api/v1/auth/refresh                              │
│         Cookie: refresh_token                                  │
│         Response: New access token + refresh token             │
│                                                                 │
│  3. Logout                                                     │
│     └─► POST /api/v1/auth/logout                               │
│         Invalidates session                                    │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Login:**
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john.doe.12345",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6...",
  "expiresIn": 300,
  "tokenType": "Bearer"
}
```

### 1.3 Password Reset

```
┌────────────────────────────────────────────────────────────────┐
│                    PASSWORD RESET FLOW                         │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Customer requests password reset                           │
│     └─► POST /api/v1/users/auth/password/request-reset         │
│         Body: { email }                                        │
│         Response: "Email verification sent"                    │
│                                                                 │
│  2. Customer receives reset link/code via email                │
│                                                                 │
│  3. Customer submits new password                              │
│     └─► PUT /api/v1/users/auth/password/reset                  │
│         Body: { email, otp, newPassword }                      │
│         Response: "Password updated successfully"              │
└────────────────────────────────────────────────────────────────┘
```

### 1.4 Browse and Book Appointment

```
┌────────────────────────────────────────────────────────────────┐
│                   BOOKING APPOINTMENT                          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Browse available branches                                  │
│     └─► GET /api/v1/location-service/branches/search?area=...  │
│         Response: List of branches with location details       │
│                                                                 │
│  2. View available slots for a branch                          │
│     └─► GET /api/v1/slots/branches/{branchId}/week             │
│         Query: ?fromDate=2026-02-24&status=AVAILABLE           │
│         Response: Slots grouped by day for next 7 days         │
│                                                                 │
│     └─► GET /api/v1/slots/branches/{branchId}/daily            │
│         Query: ?date=2026-02-24                                │
│         Response: All slots for specific date                  │
│                                                                 │
│  3. Book an appointment                                        │
│     └─► POST /api/v1/appointments/create                       │
│         Body: {                                                │
│           slotId, branchId, customerUsername,                  │
│           serviceType, day, startTime, endTime                 │
│         }                                                      │
│         Response: Created appointment with reference number    │
│                                                                 │
│  ✓ Status: BOOKED                                              │
│  ✓ Email confirmation sent to customer                         │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Search Branches:**
```bash
GET /api/v1/location-service/branches/search?area=Cape%20Town&limit=10
Authorization: Bearer {access_token}
```

**Example Request - View Weekly Slots:**
```bash
GET /api/v1/slots/branches/CPT-001/week?fromDate=2026-02-24&status=AVAILABLE
Authorization: Bearer {access_token}
```

**Example Request - Book Appointment:**
```bash
POST /api/v1/appointments/create
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "slotId": "slot-uuid-here",
  "branchId": "CPT-001",
  "customerUsername": "john.doe.12345",
  "serviceType": "ACCOUNT_INQUIRY",
  "day": "2026-02-24",
  "startTime": "09:00",
  "endTime": "09:30"
}
```

**Response:**
```json
{
  "appointmentId": "appt-uuid-here",
  "referenceNumber": "APT-20260224-001",
  "status": "BOOKED",
  "branchId": "CPT-001",
  "day": "2026-02-24",
  "startTime": "09:00",
  "endTime": "09:30",
  "serviceType": "ACCOUNT_INQUIRY",
  "createdAt": "2026-02-23T10:30:00Z"
}
```

### 1.5 Manage Appointments

```
┌─────────────���──────────────────────────────────────────────────┐
│                  MANAGE APPOINTMENTS                           │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  View My Appointments                                          │
│  └─► GET /api/v1/appointments/customer/{username}              │
│      Query: ?status=BOOKED&offset=0&limit=50                   │
│      Response: Paginated list of appointments                  │
│                                                                 │
│  View Appointment Details                                      │
│  └─► GET /api/v1/appointments/{appointmentId}                  │
│      Response: Full appointment details                        │
│                                                                 │
│  Cancel Appointment                                            │
│  └─► PATCH /api/v1/appointments/{appointmentId}/cancel         │
│      Body: { reason: "No longer needed" }                      │
│      Response: Updated appointment (status: CANCELLED)         │
│                                                                 │
│  Reschedule Appointment                                        │
│  └─► PATCH /api/v1/appointments/{appointmentId}/reschedule     │
│      Body: { newSlotId, newDay, newStartTime, newEndTime }     │
│      Response: Updated appointment with new slot               │
│      ✓ Status changes: BOOKED → RESCHEDULED → BOOKED (new)    │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Cancel Appointment:**
```bash
PATCH /api/v1/appointments/{appointmentId}/cancel
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "reason": "Schedule conflict - will rebook later"
}
```

**Example Request - Reschedule Appointment:**
```bash
PATCH /api/v1/appointments/{appointmentId}/reschedule
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "newSlotId": "new-slot-uuid",
  "newDay": "2026-02-25",
  "newStartTime": "14:00",
  "newEndTime": "14:30"
}
```

### 1.6 Check-in

```
┌────────────────────────────────────────────────────────────────┐
│               APPOINTMENT DAY - CHECK-IN                       │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Customer arrives at branch and self-checks-in                 │
│  └─► PATCH /api/v1/appointments/{appointmentId}/check-in       │
│      Response: Updated appointment (status: CHECKED_IN)        │
│                                                                 │
│  ✓ Customer now visible in branch queue                        │
│  ✓ Waiting for staff to start service                          │
│                                                                 │
│  Possible outcomes:                                            │
│  • Staff calls → IN_PROGRESS → COMPLETED                       │
│  • Customer doesn't show → NO_SHOW                             │
│  • Check-in too late → EXPIRED                                 │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Check-in:**
```bash
PATCH /api/v1/appointments/{appointmentId}/check-in
Authorization: Bearer {access_token}
```

---

## 2. Staff/Consultant Flow (app_staff)

Staff members (role: `app_staff`) serve customers at the branch.

### 2.1 View Branch Queue

```
┌────────────────────────────────────────────────────────────────┐
│                    VIEW BRANCH QUEUE                           │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_staff                                      │
│                                                                 │
│  View today's appointments for the branch                      │
│  └─► GET /api/v1/staff/appointments/branches/{branchId}        │
│      Query: ?date=2026-02-23&status=CHECKED_IN                 │
│      Response: List of checked-in customers waiting            │
│                                                                 │
│  View all statuses:                                            │
│  └─► Query: ?date=2026-02-23 (no status filter)                │
│      Response: All appointments for the day                    │
│                                                                 │
│  Appointment statuses visible:                                 │
│  • BOOKED - Upcoming, customer not yet arrived                 │
│  • CHECKED_IN - Customer waiting in queue                      │
│  • IN_PROGRESS - Currently being served                        │
│  • COMPLETED - Service finished                                │
│  • NO_SHOW - Customer didn't arrive                            │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - View Queue:**
```bash
GET /api/v1/staff/appointments/branches/CPT-001?date=2026-02-23&status=CHECKED_IN
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "appointments": [
    {
      "appointmentId": "appt-uuid-1",
      "referenceNumber": "APT-20260223-001",
      "customerUsername": "john.doe.12345",
      "customerName": "John Doe",
      "serviceType": "ACCOUNT_INQUIRY",
      "startTime": "09:00",
      "status": "CHECKED_IN",
      "checkedInAt": "2026-02-23T08:55:00Z"
    },
    {
      "appointmentId": "appt-uuid-2",
      "referenceNumber": "APT-20260223-002",
      "customerUsername": "jane.smith.67890",
      "customerName": "Jane Smith",
      "serviceType": "LOAN_APPLICATION",
      "startTime": "09:30",
      "status": "CHECKED_IN",
      "checkedInAt": "2026-02-23T09:25:00Z"
    }
  ],
  "total": 2
}
```

### 2.2 Serve Customer

```
┌────────────────────────────────────────────────────────────────┐
│                     SERVE CUSTOMER                             │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_staff                                      │
│                                                                 │
│  1. Start serving a checked-in customer                        │
│     └─► PATCH /api/v1/staff/appointments/{id}/start            │
│         Body: { consultantId: "staff-username" }               │
│         Response: Updated appointment (status: IN_PROGRESS)    │
│         ✓ assignedConsultantId set                             │
│         ✓ inProgressAt timestamp recorded                      │
│                                                                 │
│  2. Complete the appointment after service                     │
│     └─► PATCH /api/v1/staff/appointments/{id}/complete         │
│         Body: { serviceNotes: "Account inquiry resolved" }     │
│         Response: Updated appointment (status: COMPLETED)      │
│         ✓ completedAt timestamp recorded                       │
│         ✓ serviceNotes saved                                   │
│                                                                 │
│  Alternative: Mark as No-Show                                  │
│  └─► PATCH /api/v1/staff/appointments/{id}/no-show             │
│      Body: { notes: "Customer did not arrive" }                │
│      Response: Updated appointment (status: NO_SHOW)           │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Start Service:**
```bash
PATCH /api/v1/staff/appointments/{appointmentId}/start
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "consultantId": "consultant.jane"
}
```

**Example Request - Complete Appointment:**
```bash
PATCH /api/v1/staff/appointments/{appointmentId}/complete
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "serviceNotes": "Account inquiry resolved. Customer requested new debit card - processed successfully."
}
```

**Example Request - Mark No-Show:**
```bash
PATCH /api/v1/staff/appointments/{appointmentId}/no-show
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "notes": "Customer did not arrive within 15 minutes of appointment time"
}
```

### 2.3 Staff Availability

```
┌────────────────────────────────────────────────────────────────┐
│                  STAFF STATUS MANAGEMENT                       │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_staff                                      │
│                                                                 │
│  View staff members at branch                                  │
│  └─► GET /api/v1/staff/branches/{branchId}                     │
│      Query: ?status=AVAILABLE                                  │
│      Response: List of available staff usernames               │
│                                                                 │
│  Staff statuses:                                               │
│  • AVAILABLE - Ready to serve customers                        │
│  • BUSY - Currently serving                                    │
│  • ON_BREAK - Temporarily unavailable                          │
│  • OFF_DUTY - Not working                                      │
└────────────────────────────────────────────────────────────────┘
```

---

## 3. Admin/Branch Manager Flow (app_admin)

Administrators (role: `app_admin`) manage branch configuration, staff, and slots.

### 3.1 Branch Management

```
┌────────────────────────────────────────────────────────────────┐
│                   BRANCH MANAGEMENT                            │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_admin                                      │
│                                                                 │
│  Add a new branch                                              │
│  └─► POST /api/v1/branches                                     │
│      Body: { branchId: "CPT-001" }                             │
│      Response: Created branch details                          │
│                                                                 │
│  View branch details                                           │
│  └─► GET /api/v1/branches/{branchId}                           │
│      Response: Branch with appointment info and hours          │
│                                                                 │
│  List all branches                                             │
│  └─► GET /api/v1/branches                                      │
│      Query: ?offset=0&limit=20                                 │
│      Response: Paginated list of branches                      │
│                                                                 │
│  Delete a branch                                               │
│  └─► DELETE /api/v1/branches/{branchId}                        │
│      Response: 204 No Content                                  │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Add Branch:**
```bash
POST /api/v1/branches
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "branchId": "CPT-001"
}
```

### 3.2 Configure Branch Appointments

```
┌────────────────────────────────────────────────────────────────┐
│              BRANCH APPOINTMENT CONFIGURATION                  │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_admin                                      │
│                                                                 │
│  Set appointment info for a specific day type                  │
│  └─► PUT /api/v1/branches/{branchId}/appointment-info/{dayType}│
│                                                                 │
│  Day Types: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY,      │
│             SATURDAY, SUNDAY, PUBLIC_HOLIDAY                   │
│                                                                 │
│  Example: Configure Monday appointments                        │
│  └─► PUT /api/v1/branches/CPT-001/appointment-info/MONDAY      │
│      Body: {                                                   │
│        staffCount: 5,                                          │
│        slotDuration: 30,        // minutes                     │
│        utilizationFactor: 0.8,  // 80% capacity               │
│        maxBookingCapacity: 50   // max appointments/day        │
│      }                                                         │
│      Response: 200 OK                                          │
│                                                                 │
│  ✓ This affects slot generation for Mondays                    │
│  ✓ Different configurations per day type                       │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Configure Monday Appointments:**
```bash
PUT /api/v1/branches/CPT-001/appointment-info/MONDAY
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "staffCount": 5,
  "slotDuration": 30,
  "utilizationFactor": 0.8,
  "maxBookingCapacity": 50
}
```

### 3.3 Override Operation Hours

```
┌────────────────────────────────────────────────────────────────┐
│              OPERATION HOURS OVERRIDE                          │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_admin                                      │
│                                                                 │
│  Override hours for a specific date (e.g., early closing)      │
│  └─► PUT /api/v1/branches/{branchId}/operation-hours-override/ │
│         {effectiveDate}                                        │
│                                                                 │
│  Example 1: Early closing on 2026-02-28                        │
│  └─► PUT /api/v1/branches/CPT-001/operation-hours-override/    │
│         2026-02-28                                             │
│      Body: {                                                   │
│        openTime: "08:00",                                      │
│        closingTime: "14:00",    // Early closing               │
│        isClosed: false,                                        │
│        reason: "Staff training afternoon"                      │
│      }                                                         │
│                                                                 │
│  Example 2: Branch closed on specific date                     │
│  └─► PUT /api/v1/branches/CPT-001/operation-hours-override/    │
│         2026-03-01                                             │
│      Body: {                                                   │
│        openTime: null,                                         │
│        closingTime: null,                                      │
│        isClosed: true,                                         │
│        reason: "Branch renovation"                             │
│      }                                                         │
│                                                                 │
│  ✓ Affects slot availability for the specific date             │
│  ✓ Existing bookings may need manual handling                  │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Early Closing:**
```bash
PUT /api/v1/branches/CPT-001/operation-hours-override/2026-02-28
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "openTime": "08:00",
  "closingTime": "14:00",
  "isClosed": false,
  "reason": "Staff training afternoon"
}
```

**Example Request - Branch Closed:**
```bash
PUT /api/v1/branches/CPT-001/operation-hours-override/2026-03-01
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "openTime": null,
  "closingTime": null,
  "isClosed": true,
  "reason": "Branch renovation"
}
```

### 3.4 Staff Management

```
┌────────────────────────────────────────────────────────────────┐
│                    STAFF MANAGEMENT                            │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_admin                                      │
│                                                                 │
│  Add staff member to branch                                    │
│  └─► POST /api/v1/staff                                        │
│      Body: {                                                   │
│        username: "john.consultant",                            │
│        branchId: "CPT-001"                                     │
│      }                                                         │
│      Response: 204 No Content                                  │
│                                                                 │
│  View branch staff                                             │
│  └─► GET /api/v1/staff/branches/{branchId}                     │
│      Query: ?status=AVAILABLE                                  │
│      Response: Set of staff usernames                          │
│                                                                 │
│  Update staff status                                           │
│  └─► PATCH /api/v1/staff/{username}/status                     │
│      Body: { status: "ON_BREAK" }                              │
│      Response: Updated staff info                              │
│                                                                 │
│  Staff Statuses:                                               │
│  • AVAILABLE - Can serve customers                             │
│  • BUSY - Currently serving                                    │
│  • ON_BREAK - Temporary break                                  │
│  • OFF_DUTY - Not working today                                │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Add Staff:**
```bash
POST /api/v1/staff
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "username": "john.consultant",
  "branchId": "CPT-001"
}
```

**Example Request - Update Staff Status:**
```bash
PATCH /api/v1/staff/john.consultant/status
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "status": "ON_BREAK"
}
```

### 3.5 Slot Management

```
┌────────────────────────────────────────────────────────────────┐
│                     SLOT MANAGEMENT                            │
├────────────────────────────────────────────────────────────────┤
│  Required Role: app_admin                                      │
│                                                                 │
│  View slots for branch                                         │
│  └─► GET /api/v1/slots/branches/{branchId}/daily               │
│      Query: ?date=2026-02-24                                   │
│      Response: All slots for the date                          │
│                                                                 │
│  Block a slot (prevent bookings)                               │
│  └─► PATCH /api/v1/slots/{slotId}/block                        │
│      Response: Updated slot (status: BLOCKED)                  │
│      ✓ Slot no longer available for booking                    │
│                                                                 │
│  Release a blocked slot                                        ���
│  └─► PATCH /api/v1/slots/{slotId}/release                      │
│      Response: Updated slot (status: AVAILABLE)                │
│      ✓ Slot available for booking again                        │
│                                                                 │
│  Generate slots (triggered automatically or manually)          │
│  └─► POST /api/v1/admin/slots/generate                         │
│      Body: { branchId, date }                                  │
│      Response: Generated slots for the date                    │
└────────────────────────────────────────────────────────────────┘
```

**Example Request - Block Slot:**
```bash
PATCH /api/v1/slots/{slotId}/block
Authorization: Bearer {access_token}
```

**Example Request - Generate Slots:**
```bash
POST /api/v1/admin/slots/generate
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "branchId": "CPT-001",
  "date": "2026-02-25"
}
```

---

## API Role Requirements

Quick reference for role requirements per endpoint:

| Endpoint Pattern | Required Role |
|-----------------|---------------|
| `POST /api/v1/users/auth/register` | None (public) |
| `POST /api/v1/auth/login` | None (public) |
| `POST /api/v1/auth/logout` | `app_user` |
| `GET /api/v1/appointments/*` | `app_user` |
| `POST /api/v1/appointments/create` | `app_user` |
| `PATCH /api/v1/appointments/*/cancel` | `app_user` |
| `PATCH /api/v1/appointments/*/reschedule` | `app_user` |
| `PATCH /api/v1/appointments/*/check-in` | `app_user` |
| `GET /api/v1/slots/*` | `app_user` |
| `GET /api/v1/staff/appointments/*` | `app_staff` |
| `PATCH /api/v1/staff/appointments/*/start` | `app_staff` |
| `PATCH /api/v1/staff/appointments/*/complete` | `app_staff` |
| `PATCH /api/v1/staff/appointments/*/no-show` | `app_staff` |
| `POST /api/v1/branches` | `app_admin` |
| `PUT /api/v1/branches/*/appointment-info/*` | `app_admin` |
| `PUT /api/v1/branches/*/operation-hours-override/*` | `app_admin` |
| `POST /api/v1/staff` | `app_admin` |
| `PATCH /api/v1/staff/*/status` | `app_admin` |
| `PATCH /api/v1/slots/*/block` | `app_admin` |
| `PATCH /api/v1/slots/*/release` | `app_admin` |

---

## Appointment Status Flow

```
                    ┌─────────┐
                    │ BOOKED  │
                    └────┬────┘
                         │
            ┌────────────┼────────────┐
            │            │            │
            ▼            ▼            ▼
     ┌──────────┐  ┌──────────┐  ┌───────────┐
     │CANCELLED │  │CHECKED_IN│  │ EXPIRED   │
     └──────────┘  └────┬─────┘  └───────────┘
                        │
               ┌────────┴────────┐
               │                 │
               ▼                 ▼
        ┌─────────────┐    ┌─────────┐
        │ IN_PROGRESS │    │ NO_SHOW │
        └──────┬──────┘    └─────────┘
               │
               ▼
        ┌───────────┐
        │ COMPLETED │
        └───────────┘
```

**Status Descriptions:**
- **BOOKED** - Appointment created, waiting for appointment day
- **CANCELLED** - Cancelled by customer or staff
- **CHECKED_IN** - Customer arrived and checked in at branch
- **IN_PROGRESS** - Staff started serving the customer
- **COMPLETED** - Service finished successfully
- **NO_SHOW** - Customer didn't arrive for appointment
- **EXPIRED** - Appointment time passed without check-in

---

*Last updated: February 2026*

