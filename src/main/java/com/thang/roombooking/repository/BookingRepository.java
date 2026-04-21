package com.thang.roombooking.repository;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;


@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.bookingTimeSlots bts
        JOIN FETCH bts.timeSlot ts
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithTimeSlots(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.user u
        JOIN FETCH b.classroom c
        JOIN FETCH c.building bl
        LEFT JOIN FETCH b.bookingTimeSlots bts
        LEFT JOIN FETCH bts.timeSlot ts
        WHERE b.id = :id
        """)
    Optional<Booking> findByIdWithAdminDetail(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.classroom.id = :classroomId " +
           "AND b.status IN :statuses " +
           "AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime > :currentTime))")
    boolean hasUpcomingBookings(@Param("classroomId") Long classroomId,
                                @Param("statuses") List<BookingStatus> statuses,
                                @Param("today") LocalDate today,
                                @Param("currentTime") LocalTime currentTime);

    long countByUserIdAndBookingDateAndStatusNot(Long userId, LocalDate date, BookingStatus bookingStatus);

    long countByUserIdAndBookingDateAndStatusIn(Long userId, LocalDate date, List<BookingStatus> bookingStatuses);

    @Query("""
        SELECT COUNT(DISTINCT bts.id)
        FROM Booking b
        JOIN b.bookingTimeSlots bts
        WHERE b.user.id = :userId
          AND b.bookingDate = :date
          AND b.status IN :statuses
        """)
    long countBookedSlotsByUserAndDateAndStatuses(@Param("userId") Long userId,
                                                  @Param("date") LocalDate date,
                                                  @Param("statuses") List<BookingStatus> statuses);

    @Query("""
        SELECT DISTINCT b.id
        FROM Booking b
        JOIN b.bookingTimeSlots bts
        WHERE b.user.id = :userId
          AND b.bookingDate = :date
          AND b.status IN :statuses
          AND bts.timeSlot.id IN :requestedSlotIds
    """)
    List<Long> findConflictingBookingIds(@Param("userId") Long userId,
                                         @Param("date") LocalDate date,
                                         @Param("statuses") List<BookingStatus> statuses,
                                         @Param("requestedSlotIds") List<Integer> requestedSlotIds);

    @Query("""
        SELECT DISTINCT ts.id
        FROM Booking b
        JOIN b.bookingTimeSlots bts
        JOIN bts.timeSlot ts
        WHERE b.user.id = :userId
          AND b.bookingDate = :date
          AND b.status IN :statuses
        """)
    Set<Integer> findActiveSlotIdsByUserAndDateAndStatuses(@Param("userId") Long userId,
                                                           @Param("date") LocalDate date,
                                                           @Param("statuses") List<BookingStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :newStatus, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.status = 'PENDING' AND b.version = :expectedVersion")
    int atomicApprove(@Param("id") Long id,
                      @Param("newStatus") BookingStatus newStatus,
                      @Param("expectedVersion") Integer expectedVersion);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :newStatus, b.rejectionReason = :reason, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.status = 'PENDING' AND b.version = :expectedVersion")
    int atomicRejectPending(@Param("id") Long id,
                            @Param("newStatus") BookingStatus newStatus,
                            @Param("reason") String reason,
                            @Param("expectedVersion") Integer expectedVersion);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = 'CHECKED_IN', b.checkinTime = :checkinTime, b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'APPROVED' " + // Chỉ cho phép check-in khi đã được duyệt
            "AND b.version = :version")    // Chống duplicate/conflict request
    int atomicCheckIn(@Param("id") Long id,
                      @Param("checkinTime") Instant checkinTime,
                      @Param("version") Integer version);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.checkoutTime = :checkoutTime, b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'CHECKED_IN' " +
            "AND b.checkoutTime IS NULL " +
            "AND b.version = :version")
    int atomicCheckout(@Param("id") Long id,
                       @Param("checkoutTime") Instant checkoutTime,
                       @Param("version") Integer version);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = 'COMPLETED', b.checkoutTime = :checkoutTime, b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'CHECKED_IN' " +
            "AND b.checkoutTime IS NULL " +
            "AND b.version = :version")
    int atomicCheckoutToCompleted(@Param("id") Long id,
                                  @Param("checkoutTime") Instant checkoutTime,
                                  @Param("version") Integer version);

    @Query("""
    SELECT b FROM Booking b
    WHERE b.status = :status
      AND b.checkoutTime IS NULL
      AND (
        b.bookingDate < :today OR 
        (b.bookingDate = :today AND 
          (SELECT MAX(ts.endTime) FROM BookingTimeSlot bts JOIN bts.timeSlot ts WHERE bts.booking = b) <= :thresholdTime)
      )
    """)
    List<Booking> findCheckedInBookingsToAutoCheckout(@Param("status") BookingStatus status,
                                                      @Param("today") LocalDate today,
                                                      @Param("thresholdTime") LocalTime thresholdTime);

    @Query("""
    SELECT b FROM Booking b
    WHERE b.status = :status
      AND (
        b.bookingDate < :today OR 
        (b.bookingDate = :today AND 
          (SELECT MIN(ts.startTime) FROM BookingTimeSlot bts JOIN bts.timeSlot ts WHERE bts.booking = b) < :thresholdTime)
      )
    """)
    List<Booking> findExpiredBookings(@Param("status") BookingStatus status,
                                      @Param("today") LocalDate today,
                                      @Param("thresholdTime") LocalTime thresholdTime);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :newStatus, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.status = 'APPROVED' AND b.version = :expectedVersion")
    int atomicCancel(@Param("id") Long id,
                     @Param("newStatus") BookingStatus newStatus,
                     @Param("expectedVersion") Integer expectedVersion);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :newStatus, b.version = b.version + 1 " +
            "WHERE b.id = :id AND b.version = :expectedVersion AND b.status != 'CANCELLED'")
    int atomicCancelByStudent(@Param("id") Long id,
                              @Param("newStatus") BookingStatus newStatus,
                              @Param("expectedVersion") Integer expectedVersion);

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
        AND b.status IN ('APPROVED', 'PENDING', 'CHECKED_IN')
        """)
    Long countUpcomingByUser(Long userId, LocalDate today);

    @Query("""
        SELECT COUNT(b)
        FROM Booking b
        WHERE b.user.id = :userId
        """)
    Long countTotalByUser(Long userId);

    @Query("""
    SELECT DISTINCT b FROM Booking b
    JOIN FETCH b.classroom c
    JOIN FETCH b.bookingTimeSlots bts
    JOIN FETCH bts.timeSlot ts
    WHERE b.user.id = :userId
    AND (b.bookingDate < :today OR b.status NOT IN ('PENDING', 'APPROVED'))
    ORDER BY b.bookingDate DESC, b.id DESC
    """)
    List<Booking> findRecentBookings(Long userId, LocalDate today, Pageable pageable);

    @Query("""
    SELECT DISTINCT b FROM Booking b 
    JOIN FETCH b.classroom c 
    JOIN FETCH b.bookingTimeSlots bts
    JOIN FETCH bts.timeSlot ts
    WHERE b.user.id = :userId 
    AND b.bookingDate >= :today 
    AND b.status IN ('APPROVED', 'PENDING', 'CHECKED_IN')
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
