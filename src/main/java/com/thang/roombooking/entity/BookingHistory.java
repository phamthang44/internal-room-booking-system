package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseAuditEntity;
import com.thang.roombooking.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "booking_histories")
public class BookingHistory extends BaseAuditEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "action")
    private String action;

    @Column(name = "status_after")
    @Enumerated(EnumType.STRING)
    private BookingStatus statusAfter;

    @Column(name = "performed_by")
    private String performedBy;

    private String note;

}
