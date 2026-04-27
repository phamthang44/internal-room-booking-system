package com.thang.roombooking.entity;

import com.thang.roombooking.common.enums.ViolationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "violation_daily_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", length = 50, nullable = false)
    private ViolationType violationType;

    @Column(name = "violation_count")
    private long violationCount;

    @Column(name = "total_severity_pts")
    private long totalSeverityPts;

    @Column(name = "computed_at")
    private Instant computedAt;
}
