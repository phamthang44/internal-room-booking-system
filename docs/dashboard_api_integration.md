# Dashboard Integration Guide

This document outlines the API contracts for both the Admin and Student dashboards.

---

## 1. Admin Dashboard Overview

Provides high-level statistics and recent activity for administrators and staff.

### Endpoint
- **URL**: `/api/v1/admin/dashboard/overview`
- **Method**: `GET`
- **Roles**: `ADMIN`, `STAFF`
- **Response**: `ApiResult<AdminDashboardOverviewResponse>`

### Data Structure (TypeScript)
```typescript
interface AdminDashboardOverviewResponse {
  pendingApprovalCount: number;
  bookingsTodayCount: number;
  activeRoomsCount: number;
  activePenaltyCount: number;
  bookingStatusBreakdown: Record<string, number>; // e.g., { "APPROVED": 10, "PENDING": 5 }
  recentViolations: RecentViolationSummary[];
}

interface RecentViolationSummary {
  violationId: number;
  userEmail: string;
  studentCode: string;
  violationType: string;
  severityPoints: number;
  createdAt: string; // ISO-8601
}
```

---

## 2. Student Dashboard

Provides a personalized overview of bookings and history for the logged-in student.

### Endpoint
- **URL**: `/api/v1/students/dashboard`
- **Method**: `GET`
- **Auth**: Required (Uses current session)
- **Response**: `ApiResult<StudentDashboardResponse>`

### Data Structure (TypeScript)
```typescript
interface StudentDashboardResponse {
  totalBookings: number;
  upcomingBookings: number;
  pendingBookings: number;
  upcomingList: BookingSummaryResponse[];
  historyList: BookingRecentSummaryResponse[];
}

interface BookingSummaryResponse {
  bookingId: number;
  classroomName: string;
  buildingName: string;
  bookingDate: string; // ISO Date "YYYY-MM-DD"
  timeSlotRange: string; // e.g., "07:00 - 09:00"
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CHECKED_IN' | 'CHECKED_OUT' | 'CANCELLED' | 'EXPIRED';
  nextAction?: 'CHECK_IN' | 'CHECK_OUT'; // UI should show button based on this
}

interface BookingRecentSummaryResponse {
  bookingId: number;
  classroomName: string;
  buildingName: string;
  action: string; // Last action performed (e.g., "CHECK_OUT")
  statusAfter: string;
  timestamp: string; // ISO-8601
  message: string; // Human-readable activity message
}
```

---

## 3. Implementation Notes

- **Next Action Logic**: For the student dashboard, the `nextAction` field is calculated by the backend. If it's `CHECK_IN` or `CHECK_OUT`, the frontend should render the corresponding action button for that booking.
- **Refresh Strategy**: Dashboards should typically be refreshed when the user navigates back to the home/dashboard view or after performing a critical action (like checking in).
- **Empty States**: If `upcomingList` or `historyList` are empty, the UI should show a "No recent activity" or "No upcoming bookings" placeholder.
