package com.thang.roombooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "room_utilization_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomUtilizationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "booked_slot_count")
    private int bookedSlotCount;

    @Column(name = "total_slot_capacity")
    private int totalSlotCapacity;

    @Column(name = "utilization_pct", precision = 5, scale = 2)
    private BigDecimal utilizationPct;

    @Column(name = "total_bookings")
    private long totalBookings;

    @Column(name = "avg_attendees", precision = 5, scale = 2)
    private BigDecimal avgAttendees;

    @Column(name = "computed_at")
    private Instant computedAt;
}
