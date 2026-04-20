# Admin Booking API Contract

This document outlines the API endpoints and WebSocket channels for managing booking requests from the Admin/Staff perspective.

## 1. REST Endpoints
**Base URL:** `/api/v1/admin/bookings`

### 1.1 Approve Booking
Updates the status of a booking to `APPROVED`.
- **Endpoint:** `PATCH /approve`
- **Security:** `hasAnyRole('ADMIN', 'STAFF')`
- **Request Body:** `BookingApprovalRequest`
- **Success Response:** `200 OK` with success message.

### 1.2 Reject Booking
Updates the status of a booking to `REJECTED`.
- **Endpoint:** `PATCH /reject`
- **Security:** `hasAnyRole('ADMIN', 'STAFF')`
- **Request Body:** `BookingApprovalRequest` (Reason is mandatory for rejection).
- **Success Response:** `200 OK`

### 1.3 Search Bookings
Advanced search with filters and pagination.
- **Endpoint:** `GET /`
- **Security:** `hasAnyRole('ADMIN', 'STAFF')`
- **Query Parameters:**
  - `bookingId` (Long): Exact lookup.
  - `studentCode` (String): Search by student identifier.
  - `classroomId` (Long): Filter by room.
  - `status` (Enum): `PENDING`, `APPROVED`, etc.
  - `bookingDate` (LocalDate): Format `YYYY-MM-DD`.
  - `page` (int): 1-indexed (Default: 1).
  - `size` (int): Items per page (Default: 20).
  - `sort` (Enum): `NEWEST`, `OLDEST`, etc.

---

## 2. Data Types (DTOs)

### 2.1 BookingApprovalRequest
```json
{
  "bookingId": 123,
  "action": "APPROVE", // or "REJECT"
  "reason": "String (Max 500 characters)"
}
```

### 2.2 AdminBookingSearchRequest
Managed via Query Parameters (mapped to `@ModelAttribute`).

---

## 3. WebSocket Real-time Notifications

Whenever a booking status changes (Approve/Reject/Check-in/Cancel), the system pushes real-time updates.

### 3.1 Admin Channel (Global Update)
Admins should subscribe to this topic to refresh their Dashboard Booking List in real-time.
- **Topic:** `/topic/admin/bookings`
- **Purpose:** Inform all online staff that a record has changed.

### 3.2 Student Channel (Personal Notification)
Students receive personalized "Toasts" when their specific booking is processed.
- **Topic:** `/topic/notifications/{userId}`
- **Purpose:** Alert the user of approval/rejection results.

### 3.3 Payload Structure (`NotificationPayload`)
```json
{
  "type": "BOOKING_APPROVED", // or BOOKING_REJECTED, etc.
  "title": "Booking Approved",
  "message": "Congratulations! Your booking for room A101 has been approved.",
  "bookingId": 123,
  "status": "APPROVED",
  "timestamp": "ISO-8601 string"
}
```

---

## 4. Implementation Details
The notification logic is decoupled from the business service using Spring Events:
1. `BookingCommandService` publishes a `BookingStatusChangedEvent`.
2. `BookingNotificationListener` catches the event and sends it to `NotificationService`.
3. `NotificationService` pushes the payload via `SimpMessagingTemplate` to the appropriate WebSocket topics.
