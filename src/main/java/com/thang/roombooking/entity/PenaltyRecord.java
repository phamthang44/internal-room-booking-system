package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseAuditEntity;
import com.thang.roombooking.common.enums.PenaltyAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "penalty_records")
public class PenaltyRecord extends BaseAuditEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "action_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private PenaltyAction penaltyAction;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;
}
