# Equipment Administration API Integration Guide

This document outlines the API contract for the Equipment management module. All endpoints require **ADMIN** or **STAFF** roles as specified below and use the standard `ApiResult<T>` wrapper.

---

## 1. Data Models (TypeScript Types)

### Base Response Wrapper
```typescript
interface ApiResult<T> {
  data: T;
  meta: {
    serverTime: number;
    apiVersion: string;
    traceId: string;
    message?: string; // Standard success message from backend
    // Pagination (only for List)
    page?: number;
    size?: number;
    totalElements?: number;
    totalPages?: number;
  };
  error?: {
    code: string;
    message: string;
    traceId: string;
    details?: any;
  };
}
```

### Equipment Types
```typescript
interface AuditInfo {
  createdBy: string;
  createdDate: string; // ISO-8601
  lastModifiedBy: string;
  lastModifiedDate: string; // ISO-8601
}

interface EquipmentListItem {
  id: number;
  nameKey: string;     // Unique identifier (e.g. "equipment.projector")
  nameVi: string;
  nameEn: string;
  isActive: boolean;
  classroomCount: number; // Number of classrooms currently using this equipment
}

interface EquipmentDetail extends EquipmentListItem {
  descVi: string;
  descEn: string;
  audit: AuditInfo;
}

interface EquipmentRequest {
  nameVi: string;
  nameEn: string;
  descVi?: string;
  descEn?: string;
}
```

---

## 2. API Endpoints

| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/admin/equipment` | `ADMIN`, `STAFF` | List equipment with pagination and search |
| **GET** | `/api/v1/admin/equipment/{id}` | `ADMIN`, `STAFF` | Get full details of a specific equipment |
| **POST** | `/api/v1/admin/equipment` | `ADMIN` | Create new equipment |
| **PUT** | `/api/v1/admin/equipment/{id}` | `ADMIN` | Update equipment info |
| **DELETE** | `/api/v1/admin/equipment/{id}` | `ADMIN` | Deactivate (Soft-delete) |
| **PATCH** | `/api/v1/admin/equipment/{id}/reactivate` | `ADMIN` | Reactivate a deleted equipment |

---

## 3. Integration Details

### List Equipment (Paginated)
- **URL Params:**
  - `keyword`: (Optional) Filter by `nameKey`.
  - `page`: (Optional, default 0).
  - `size`: (Optional, default 20).
  - `sort`: (Optional, e.g., `nameKey,asc`).
- **Response Data:** `Page<EquipmentListItem>`

### Create/Update Equipment
- **Slugification:** The `nameKey` is automatically generated from `nameEn` on the first creation (e.g., "Air Conditioner" -> "equipment.air_conditioner"). It remains stable during updates.

### Deactivation Guard
- **Important:** The `DELETE` endpoint will return a `409 Conflict` error if the equipment is currently assigned to any **active** classroom. The frontend should display the error message provided in `error.message`.

### Success Messages
- The backend provides localized success messages in `meta.message`. It is recommended to use these for Toast notifications to ensure consistent i18n.

---

## 4. Error Codes

| Code | HTTP | Scenario |
| :--- | :--- | :--- |
| `EQUIPMENT_NOT_FOUND` | 404 | Equipment ID does not exist or is already deleted (for GET) |
| `EQUIPMENT_NAME_EXISTED`| 409 | Attempting to create an equipment with a name that generates an existing key |
| `EQUIPMENT_IN_USE` | 409 | Attempting to deactivate equipment used by classrooms |
