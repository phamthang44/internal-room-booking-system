package com.thang.roombooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dashboard_daily_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(name = "total_bookings")
    private long totalBookings;

    @Column(name = "pending_count")
    private long pendingCount;

    @Column(name = "approved_count")
    private long approvedCount;

    @Column(name = "rejected_count")
    private long rejectedCount;

    @Column(name = "cancelled_count")
    private long cancelledCount;

    @Column(name = "completed_count")
    private long completedCount;

    @Column(name = "checked_in_count")
    private long checkedInCount;

    @Column(name = "new_violations")
    private long newViolations;

    @Column(name = "no_show_count")
    private long noShowCount;

    @Column(name = "active_rooms")
    private long activeRooms;

    @Column(name = "active_penalties")
    private long activePenalties;

    @Column(name = "computed_at")
    private Instant computedAt;
}
