package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bookings")
public class Booking extends BaseSoftDeleteEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookingStatus status;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT", length = 500)
    private String purpose;

    @Column(name = "idempotency_key", length = 100, unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "rejection_reason", columnDefinition = "TEXT", length = 500)
    private String rejectionReason;

    @Version
    private Integer version;

    @Override
    public Long getId() {
        return id;
    }
}
