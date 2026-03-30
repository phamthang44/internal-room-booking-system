package com.thang.roombooking.repository;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByClassroomIdAndEndTimeAfter(Long roomId, Instant now);

    boolean existsByClassroomIdAndStatusInAndStartTimeAfter(
            Long classroomId,
            List<BookingStatus> statuses,
            Instant time
    );

    boolean existsByClassroomIdAndStatusInAndEndTimeAfter(Long roomId, List<BookingStatus> approved, Instant now);

    long countByUserIdAndBookingDateAndStatusNot(Long userId, LocalDate date, BookingStatus bookingStatus);

    long countByUserIdAndBookingDateAndStatusIn(Long userId, LocalDate date, List<BookingStatus> bookingStatuses);

    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.status = 'PENDING' AND b.version = :expectedVersion")
    int atomicApprove(@Param("id") Long id,
                      @Param("newStatus") BookingStatus newStatus,
                      @Param("expectedVersion") Integer expectedVersion);

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'CHECKED_IN', b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'APPROVED' " + // Chỉ cho phép check-in khi đã được duyệt
            "AND b.version = :version")    // Chống duplicate/conflict request
    int atomicCheckIn(Long id, Integer version);

}
