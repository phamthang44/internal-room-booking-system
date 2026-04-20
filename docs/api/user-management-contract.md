# User Management API Contract

This document outlines the API endpoints for managing users, including administrative actions and personal profile retrieval.

## 1. REST Endpoints

### 1.1 Admin Endpoints
**Base URL:** `/api/v1/admin/users`

#### Get All Users
Returns a paginated list of all users in the system.
- **Endpoint:** `GET /`
- **Security:** `hasRole('ADMIN')`
- **Query Parameters:**
  - `page` (int): 0-indexed page number (Default: 0)
  - `size` (int): Items per page (Default: 20)
  - `sort` (String): Sorting criteria (e.g., `id,desc`)
- **Success Response:** `200 OK` with `ApiResult<Page<UserBasicResponse>>`

#### Create User Account
Manually create a new user account (Staff or student).
- **Endpoint:** `POST /`
- **Security:** `hasRole('ADMIN')`
- **Request Body:** `RegisterRequest`
- **Success Response:** `200 OK` with `ApiResult<UserBasicResponse>`

#### Ban/Unban User
Toggles the active/banned status of a user.
- **Endpoint:** `PUT /{userId}/ban`
- **Security:** `hasRole('ADMIN')`
- **Path Variables:** `userId` (Long) - Internal ID of the user.
- **Success Response:** `200 OK` with success message.

#### Update User Role
Changes the role of a specific user.
- **Endpoint:** `PUT /{userId}/role`
- **Security:** `hasRole('ADMIN')`
- **Path Variables:** `userId` (Long) - Internal ID of the user.
- **Query Parameters:**
  - `roleName` (String): Name of the role (`ADMIN`, `STAFF`, `STUDENT`)
- **Success Response:** `200 OK` with updated `UserBasicResponse`

---

### 1.2 User Endpoints
**Base URL:** `/api/v1/users`

#### Get Current User Profile
Retrieves the profile information of the currently authenticated user.
- **Endpoint:** `GET /me`
- **Security:** `Authenticated`
- **Success Response:** `200 OK` with `ApiResult<UserProfileResponse>`

---

## 2. Data Types (DTOs)

### 2.1 UserBasicResponse
Used for listing and basic identifying actions.

| Field | Type | Description | Values/Example |
| :--- | :--- | :--- | :--- |
| `id` | Long | Internal unique identifier | `123` |
| `username` | String | Unique login name | `john.doe` |
| `email` | String | Contact email address | `john@example.com` |
| `role` | String | Assigned permission level | `ADMIN`, `STAFF`, `STUDENT` |
| `status` | String | Current account state | `ACTIVE`, `INACTIVE`, `BANNED` |
| `studentCode`| String | Unique student identifier | `B21DCCN123` (null for staff) |

### 2.2 UserProfileResponse
Contains detailed profile information for the "My Profile" section.

| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `id` | Long | Internal unique identifier | `123` |
| `fullName` | String | Full display name | `John Doe` |
| `username` | String | Unique login name | `john.doe` |
| `roleName` | String | Name of the primary role | `STUDENT` |
| `email` | String | Contact email address | `john@example.com` |
| `studentCode`| String | Unique student identifier | `B21DCCN123` |

### 2.3 RegisterRequest
Validation rules for account creation.

| Field | Type | Required | constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `username` | String | Yes | 3-30 chars | Unique login identifier. |
| `fullName` | String | Yes | 3-100 chars | Display name of the user. |
| `email` | String | Yes | Valid email | User's primary contact email. |
| `password` | String | Yes | Min 8 chars | Must contain: Upper, Lower, Digit, Special Char. |
| `confirmPassword`| String | Yes | Matches password | Confirmation of the chosen password. |

> [!IMPORTANT]
> **Password Complexity Regex:** `^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$`

---

## 3. Common Response Wrapper (ApiResult)

### 3.1 Metadata (`meta`)

| Field | Type | Description |
| :--- | :--- | :--- |
| `serverTime` | Long | Epoch timestamp in milliseconds. |
| `apiVersion` | String | Current API version (e.g., `1.0.0`). |
| `traceId` | String | Unique UUID for debugging/tracking. |
| `message` | String | Localized success/error message for the UI. |
| `page` | Integer | Current page index (starts at 0). |
| `size` | Integer | Number of items requested per page. |
| `totalElements` | Long | Total number of items matching the query. |
| `totalPages` | Integer | Total number of available pages. |

### 3.2 Error Detail (`error`)

| Field | Type | Description |
| :--- | :--- | :--- |
| `code` | String | Machine-readable error code (e.g., `USER_NOT_FOUND`). |
| `message` | String | Human-readable error message. |
| `traceId` | String | Reference ID to correlate with server logs. |
| `details` | Object | Map of field-level validation errors (if applicable). |

---

## 4. HTTP Status Codes

| Code | Usage |
| :--- | :--- |
| `200 OK` | Request completed successfully. |
| `400 Bad Request` | Validation failed (e.g., weak password, missing fields). |
| `401 Unauthorized` | Missing or invalid authentication token. |
| `403 Forbidden` | Authenticated user lacks `ADMIN` role. |
| `404 Not Found` | User with the specified ID does not exist. |
| `500 Server Error` | Unexpected internal processing error. |
