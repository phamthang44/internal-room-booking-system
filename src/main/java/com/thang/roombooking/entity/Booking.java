package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE public.booking SET deleted_at = NOW()")
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

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval = true)
    private List<BookingTimeSlot> bookingTimeSlots = new ArrayList<>();

    @Column(name = "booking_date", nullable = false, updatable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookingStatus status;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT", length = 500)
    private String purpose;

    @Column(name = "rejection_reason", columnDefinition = "TEXT", length = 500)
    private String rejectionReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Version
    private Integer version;

    public void addTimeSlot(BookingTimeSlot timeSlot) {
        bookingTimeSlots.add(timeSlot);
    }

    public void removeTimeSlot(BookingTimeSlot timeSlot) {
        bookingTimeSlots.remove(timeSlot);
    }

    public void updateTimeSlot(BookingTimeSlot timeSlot, int index) {
        bookingTimeSlots.set(index, timeSlot);
    }

}
