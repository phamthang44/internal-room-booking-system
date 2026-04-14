package com.thang.roombooking.repository;

import com.thang.roombooking.common.dto.response.BookingSummaryResponse;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
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
    @Query("UPDATE Booking b SET b.status = :newStatus, b.rejectionReason = :reason, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.status = 'PENDING' AND b.version = :expectedVersion")
    int atomicRejectPending(@Param("id") Long id,
                            @Param("newStatus") BookingStatus newStatus,
                            @Param("reason") String reason,
                            @Param("expectedVersion") Integer expectedVersion);

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'CHECKED_IN', b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'APPROVED' " + // Chỉ cho phép check-in khi đã được duyệt
            "AND b.version = :version")    // Chống duplicate/conflict request
    int atomicCheckIn(Long id, Integer version);

    @Query("""
    SELECT b FROM Booking b\s
    WHERE b.status = :status\s
    AND (
        b.bookingDate < :today\s
        OR (b.bookingDate = :today AND EXISTS (
            SELECT 1 FROM BookingTimeSlot bts\s
            JOIN bts.timeSlot ts\s
            WHERE bts.booking = b\s
            GROUP BY bts.booking\s
            HAVING MIN(ts.startTime) < :thresholdTime
        ))
    )\s""")
    List<Booking> findExpiredBookings(@Param("status") BookingStatus status,
                                      @Param("today") LocalDate today,
                                      @Param("thresholdTime") LocalTime thresholdTime);

    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.status = 'APPROVED' AND b.version = :expectedVersion")
    int atomicCancel(@Param("id") Long id,
                     @Param("newStatus") BookingStatus newStatus,
                     @Param("expectedVersion") Integer expectedVersion);

    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.version = :expectedVersion AND b.status != 'CANCELLED'")
    int atomicCancelByStudent(@Param("id") Long id,
                              @Param("newStatus") BookingStatus newStatus,
                              @Param("expectedVersion") Integer expectedVersion);

    @Query("""
    SELECT new com.thang.roombooking.common.dto.response.BookingSummaryResponse(
        b.id, c.roomName, c.building.nameKey, b.bookingDate, '', b.status
    )
    FROM Booking b
    JOIN b.classroom c
    WHERE b.user.id = :userId
    AND b.bookingDate >= :today
    AND b.status IN ('APPROVED', 'PENDING')
    ORDER BY b.bookingDate ASC
    """)
    List<BookingSummaryResponse> findUpcomingBookings(Long userId, LocalDate today);

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
        AND b.status = 'PENDING'
        """)
    Long countPendingByUser(Long userId);

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
        AND b.bookingDate >= :today
        AND b.status IN ('APPROVED', 'PENDING')
        """)
    Long countUpcomingByUser(Long userId, LocalDate today);

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
        """)
    Long countTotalByUser(Long userId);

    @Query("""
    SELECT b FROM Booking b
    JOIN FETCH b.classroom c
    WHERE b.user.id = :userId
    AND (b.bookingDate < :today OR b.status NOT IN ('PENDING', 'APPROVED'))
    ORDER BY b.bookingDate DESC, b.id DESC
    """)
    List<Booking> findRecentBookings(Long userId, LocalDate today, Pageable pageable);

    @Query("""
    SELECT b FROM Booking b 
    JOIN FETCH b.classroom c 
    WHERE b.user.id = :userId 
    AND b.bookingDate >= :today 
    AND b.status IN ('APPROVED', 'PENDING')
    ORDER BY b.bookingDate ASC
    """)
    List<Booking> findUpcomingBookings(@Param("userId") Long userId, @Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT DISTINCT b FROM Booking b " +
           "JOIN FETCH b.bookingTimeSlots bts " +
           "JOIN FETCH bts.timeSlot ts " +
           "WHERE b.classroom.id = :classroomId " +
           "AND b.bookingDate >= :startDate " +
           "AND b.bookingDate <= :endDate " +
           "AND b.status IN :statuses")
    List<Booking> findBookingsByClassroomAndDateRange(
            @Param("classroomId") Long classroomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT DISTINCT b FROM Booking b " +
           "JOIN FETCH b.bookingTimeSlots bts " +
           "JOIN FETCH bts.timeSlot ts " +
           "WHERE b.classroom.id IN :classroomIds " +
           "AND b.bookingDate = :date " +
           "AND b.status IN :statuses")
    List<Booking> findBookingsByClassroomIdsAndDate(
            @Param("classroomIds") List<Long> classroomIds,
            @Param("date") LocalDate date,
            @Param("statuses") List<BookingStatus> statuses);
}
