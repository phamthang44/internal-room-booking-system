# Admin Dashboard Analytics API Contract

This document covers the analytics and trend endpoints designed for chart visualization in the Admin Dashboard.

---

## 1. Booking Trends (Daily)
Provides a day-by-day breakdown of booking counts and statuses. Useful for Line or Area charts.

- **URL**: `/api/v1/admin/dashboard/trend`
- **Method**: `GET`
- **Params**: `days` (Optional, default: `30`)
- **Response**: `ApiResult<List<DailyBookingTrendResponse>>`

### TypeScript Type
```typescript
interface DailyBookingTrendResponse {
  date: string; // ISO Date "YYYY-MM-DD"
  totalBookings: number;
  pendingCount: number;
  approvedCount: number;
  rejectedCount: number;
  cancelledCount: number;
  completedCount: number;
  checkedInCount: number;
  newViolations: number;
  noShowCount: number;
}
```

---

## 2. Room Utilization Stats (Weekly)
Provides utilization percentages and occupancy metrics per classroom. Useful for Heatmaps or Bar charts.

- **URL**: `/api/v1/admin/dashboard/room-stats`
- **Method**: `GET`
- **Params**: `weeks` (Optional, default: `4`)
- **Response**: `ApiResult<List<RoomUtilizationResponse>>`

### TypeScript Type
```typescript
interface RoomUtilizationResponse {
  classroomId: number;
  roomName: string;
  weekStart: string; // ISO Date "YYYY-MM-DD" (Monday)
  bookedSlotCount: number;
  totalSlotCapacity: number;
  utilizationPct: number; // e.g., 75.50
  totalBookings: number;
  avgAttendees: number;
}
```

---

## 3. Violation Trends
Provides a temporal breakdown of rule violations by type. Useful for Stacked Bar charts.

- **URL**: `/api/v1/admin/dashboard/violation-trend`
- **Method**: `GET`
- **Params**: `weeks` (Optional, default: `8`)
- **Response**: `ApiResult<List<ViolationTrendResponse>>`

### TypeScript Type
```typescript
interface ViolationTrendResponse {
  date: string; // ISO Date "YYYY-MM-DD"
  violationType: string;
  violationCount: number;
  totalSeverityPts: number;
}
```

---

## Implementation Notes

1. **Date Formatting**: All date fields are returned in standard ISO string format (`YYYY-MM-DD`).
2. **Dynamic Range**: The `days` and `weeks` parameters allow the frontend to offer "Last 7 days", "Last 30 days", etc., toggles in the UI.
3. **Visualization Strategy**:
   - Use **Booking Trends** for the main activity line chart.
   - Use **Room Stats** to identify "dead" hours or over-capacity rooms.
   - Use **Violation Trend** to monitor if specific rules are being broken more frequently over time.
