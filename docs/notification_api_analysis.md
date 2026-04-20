# Notification Feature API Analysis

This document provides a detailed analysis of the Request and Response types for the Notification feature, as implemented in `NotificationController.java`.

## Table of Contents
1. [Overview](#overview)
2. [Base Response Structure (ApiResult)](#base-response-structure-apiresult)
3. [Notification Response DTO](#notification-response-dto)
4. [API Endpoints Analysis](#api-endpoints-analysis)
   - [Read Operations](#read-operations)
   - [Write/Clear Operations](#writeclear-operations)
5. [WebSocket Integration (Real-time)](#websocket-integration-real-time)
6. [UI Behavior Recommendations (The Bell Icon)](#ui-behavior-recommendations-the-bell-icon)
7. [Enums (NotificationType)](#enums-notificationtype)

---

## Overview
The Notification system uses a standardized `ApiResult` wrapper for all responses. It supports pagination, unread counts, and status updates (marking as read).

## Base Response Structure (ApiResult)
All responses from the `NotificationController` are wrapped in the `ApiResult<T>` class.

| Field | Type | Description |
| :--- | :--- | :--- |
| `data` | `T` | The actual payload (e.g., list of notifications, count, or null). |
| `meta` | `Meta` | Metadata including server time, pagination info, and trace ID. |
| `error` | `ErrorDetail` | Present only if an error occurs. |

### Meta Object (Pagination & System Info)
For paginated requests, the `meta` object contains:
- `page`: Current page number (1-indexed in API, 0-indexed in DB).
- `size`: Number of items per page.
- `totalElements`: Total number of notifications for the user.
- `totalPages`: Total number of pages available.
- `serverTime`: Timestamp of the response.

---

## Notification Response DTO
The payload for most notification-related responses is `NotificationResponse`.

```java
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private String relatedId; // ID of the entity this notification relates to (e.g., Booking ID)
    private Instant createdAt;
}
```

---

## API Endpoints Analysis

### Read Operations

#### 1. Get Paginated Notifications
**Endpoint:** `GET /api/v1/notifications`

**Request Parameters:**
- `page` (int, default=1): Page number.
- `size` (int, default=20): Items per page.

**Authentication:** Required.

**Response Type:** `ApiResult<List<NotificationResponse>>`

#### 2. Get Unread Count
**Endpoint:** `GET /api/v1/notifications/unread-count`

**Response Type:** `ApiResult<Long>`

---

### Write/Clear Operations

#### 3. Mark Specific Notification as Read
**Endpoint:** `PATCH /api/v1/notifications/{id}/read`

**Response Type:** `ApiResult<Void>`

#### 4. Mark All Notifications as Read
**Endpoint:** `POST /api/v1/notifications/read-all`

**Response Type:** `ApiResult<Void>`

#### 5. Delete Specific Notification
**Endpoint:** `DELETE /api/v1/notifications/{id}`

**Description**: Removes a single notification from the user's history permanently.

#### 6. Bulk Delete Notifications
**Endpoint:** `DELETE /api/v1/notifications/bulk`

**Request Body**: `List<Long>` (JSON array of IDs).

**Description**: Deletes multiple specific notifications.

#### 7. Clear All Notifications
**Endpoint:** `DELETE /api/v1/notifications/clear-all`

**Description**: Permanently wipes all notification history for the authenticated user.

---

## WebSocket Integration (Real-time)
The backend pushes real-time notifications via STOMP.

**Destination:** `/topic/notifications/{userId}`

**JSON Payload (NotificationPayload):**
The frontend should expect the following JSON structure when a message is received:

```json
{
  "type": "BOOKING_APPROVED",
  "title": "Booking Approved",
  "message": "Your booking for room A101 has been approved.",
  "bookingId": 12345,
  "status": "APPROVED",
  "timestamp": "2023-10-27T10:00:00Z"
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | `String` | Specific event type (e.g., `BOOKING_APPROVED`, `BOOKING_REJECTED`). |
| `title` | `String` | Brief title for the notification. |
| `message` | `String` | Detailed body text. |
| `bookingId` | `Long` | ID of the related booking (for navigation). |
| `status` | `String` | Current status of the booking (useful for styling). |
| `timestamp` | `Instant`| When the notification was generated. |

---

## UI Behavior Recommendations (The Bell Icon)

### 1. Initialization
- **Action**: On page load, the frontend should call `GET /api/v1/notifications/unread-count`.
- **UI**: If count > 0, display a red badge with the number over the bell icon.

### 2. Interaction (Dropdown)
- **Action**: When the user clicks the bell, call `GET /api/v1/notifications?page=1&size=10`.
- **UI**: Show a scrollable list of recent notifications. Use `isRead` to style unread items (e.g., light blue background).
- **Navigation**: Clicking an item should navigate the user to the booking detail page using the `relatedId` or `bookingId`.

### 3. Real-time Updates
- **Action**: Listen on `/topic/notifications/{userId}`.
- **UI**:
  - Increment the unread count badge.
  - Prepend the new notification to the top of the dropdown list.
  - (Optional) Trigger a small toast/browser notification for immediate visibility.

### 4. Consumption
- **Action**:
  - Clicking a specific notification -> Call `PATCH /{id}/read`.
  - Clicking "Mark all as read" -> Call `POST /read-all`.
- **UI**: 
  - Update the specific item's style (clear background).
  - Reset/decrement the unread badge count.

---

## Enums (NotificationType)
The `type` field in `NotificationResponse` uses the following enum values:

- `BOOKING_STATUS`: Notifications related to booking approvals, rejections, or updates.
- `SYSTEM_ALERT`: General system alerts or maintenance notices.
