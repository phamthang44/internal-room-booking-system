# Student Dashboard & Recommendation API Integration Guide

This document outlines the enhanced Student Dashboard and the new Recommendation System contracts.

---

## 1. Enriched Student Dashboard

Provides counters, attendance stats, penalty awareness, and booking history.

- **URL**: `/api/v1/students/dashboard`
- **Method**: `GET`
- **Role**: `STUDENT`
- **Response**: `ApiResult<StudentDashboardResponse>`

### Data Structure (TypeScript)
```typescript
interface StudentDashboardResponse {
  // --- Standard Counters ---
  totalBookings: number;
  upcomingBookings: number;
  pendingBookings: number;

  // --- Lists ---
  upcomingList: BookingSummaryResponse[];
  historyList: BookingRecentSummaryResponse[];

  // --- Attendance Stats (New) ---
  attendanceRate: number;           // 0.0 to 1.0 (e.g., 0.85)
  noShowCount: number;              // Lifetime "No Show" count
  cancelledThisMonthCount: number;  // Cancellations in rolling 30 days

  // --- Penalty Awareness (New) ---
  hasPenalty: boolean;
  penaltyLevel?: 'WARNING' | 'REQUIRE_APPROVAL' | 'BAN_TEMP' | 'PERMANENT_BAN';
  penaltyExpiresAt?: string;        // ISO-8601 (null if PERMANENT_BAN)

  // --- Quality Metrics (New) ---
  avgActualAttendees: number;       // Avg actual attendees in COMPLETED bookings
}
```

---

## 2. Room Recommendation System

Suggests rooms based on the student's historical behavior and group size.

- **URL**: `/api/v1/students/recommendations`
- **Method**: `GET`
- **Role**: `STUDENT`
- **Params**:
  - `attendees`: (Optional) Filter rooms by min capacity.
  - `date`: (Optional, `YYYY-MM-DD`) Defaults to today.
- **Response**: `ApiResult<RoomRecommendationResponse[]>`

### Data Structure (TypeScript)
```typescript
interface RoomRecommendationResponse {
  classroomId: number;
  roomName: string;
  buildingName: string;   // Localized
  roomTypeName: string;   // Localized
  capacity: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';
  bookingCount: number;   // How many times THIS user has booked THIS room
  avgActualAttendees: number;
  reasonKey: string;      // i18n key for the "Why this room?" label
}
```

---

## 3. Localization (i18n)

The `reasonKey` in recommendations should be translated using the following keys from the backend bundle:

| Key | Template | Note |
| :--- | :--- | :--- |
| `recommendation.reason.frequent` | "You have booked this room {0} times before" | `{0}` is `bookingCount` |
| `recommendation.reason.capacity_match` | "Fits your group size of {0}" | `{0}` is the `attendees` param |

---

## 4. UI Recommendations

1. **Dashboard Alerts**: If `hasPenalty` is true, display a warning banner at the top of the dashboard using the `penaltyLevel` to determine severity.
2. **Attendance Ring**: Use the `attendanceRate` to render a circular progress chart (e.g., "85% Attendance").
3. **Recommendation "Chips"**: Display the `reasonKey` as a small badge or "chip" on the room card to explain the recommendation (e.g., "Frequently Booked").
