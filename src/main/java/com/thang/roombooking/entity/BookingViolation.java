package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.ViolationSource;
import com.thang.roombooking.common.enums.ViolationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE public.booking_violations SET deleted_at = NOW()")
@Entity
@Table(name = "booking_violations")
public class BookingViolation extends BaseSoftDeleteEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private ViolationSource source = ViolationSource.SYSTEM;

    @Column(name = "severity_points")
    private Integer severityPoints = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id")
    private PenaltyRecord penalty;

    @Enumerated(EnumType.STRING)
    private ViolationType type;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "resolved_at", columnDefinition = "timestamptz DEFAULT NOW()")
    private Instant resolvedAt;

}
