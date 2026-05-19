package com.thang.roombooking.repository;

import com.thang.roombooking.common.enums.AttendanceStatus;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.bookingTimeSlots bts
        JOIN FETCH bts.timeSlot ts
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithTimeSlotsAndLock(@Param("id") Long id);

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

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.classroom.building.id = :buildingId " +
           "AND b.status IN :statuses " +
           "AND (b.bookingDate > :today OR (b.bookingDate = :today AND b.endTime > :currentTime))")
    boolean hasUpcomingBookingsForBuilding(@Param("buildingId") Long buildingId,
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
        SELECT DISTINCT b.id
        FROM Booking b
        JOIN b.bookingTimeSlots bts
        WHERE b.classroom.id = :roomId
          AND b.bookingDate = :date
          AND b.status IN :statuses
          AND bts.timeSlot.id IN :requestedSlotIds
    """)
    List<Long> findConflictingRoomBookingIds(@Param("roomId") Long roomId,
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
    @Query("""
        UPDATE Booking b
           SET b.status = :newStatus,
               b.rejectionReason = :reason,
               b.updatedBy = :updatedBy,
               b.version = b.version + 1
         WHERE b.id = :id
           AND b.status = 'PENDING'
           AND b.version = :expectedVersion
    """)
    int atomicRejectPendingBySystem(@Param("id") Long id,
                                    @Param("newStatus") BookingStatus newStatus,
                                    @Param("reason") String reason,
                                    @Param("updatedBy") String updatedBy,
                                    @Param("expectedVersion") Integer expectedVersion);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Booking b SET b.status = 'CHECKED_IN', b.checkinTime = :checkinTime, b.attendanceStatus = 'ATTENDED', b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'APPROVED' " +
            "AND b.version = :version")
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
    @Query("UPDATE Booking b SET b.status = 'COMPLETED', b.checkoutTime = :checkoutTime, b.actualAttendees = :actualAttendees, b.version = b.version + 1 " +
            "WHERE b.id = :id " +
            "AND b.status = 'CHECKED_IN' " +
            "AND b.checkoutTime IS NULL " +
            "AND b.version = :version")
    int atomicCheckoutToCompleted(@Param("id") Long id,
                                  @Param("checkoutTime") Instant checkoutTime,
                                  @Param("actualAttendees") Integer actualAttendees,
                                  @Param("version") Integer version);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Booking b
           SET b.status = 'COMPLETED',
               b.checkoutTime = :checkoutTime,
               b.updatedBy = :updatedBy,
               b.version = b.version + 1
         WHERE b.id = :id
           AND b.status = 'CHECKED_IN'
           AND b.checkoutTime IS NULL
           AND b.version = :version
    """)
    int atomicAutoCheckoutToCompleted(@Param("id") Long id,
                                      @Param("checkoutTime") Instant checkoutTime,
                                      @Param("updatedBy") String updatedBy,
                                      @Param("version") Integer version);

    @Query("""
    SELECT b FROM Booking b
    WHERE b.status = :status
      AND b.checkoutTime IS NULL
      AND (
        b.bookingDate < :today OR 
        (b.bookingDate = :today AND 
          COALESCE((SELECT MAX(ts.endTime) FROM BookingTimeSlot bts JOIN bts.timeSlot ts WHERE bts.booking = b), b.endTime) <= :thresholdTime)
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
          COALESCE((SELECT MIN(ts.startTime) FROM BookingTimeSlot bts JOIN bts.timeSlot ts WHERE bts.booking = b), b.startTime) <= :thresholdTime)
      )
    """)
    List<Booking> findPendingBookingsToAutoReject(@Param("status") BookingStatus status,
                                                  @Param("today") LocalDate today,
                                                  @Param("thresholdTime") LocalTime thresholdTime);

    @Query("""
    SELECT DISTINCT b FROM Booking b
    LEFT JOIN FETCH b.bookingTimeSlots bts
    LEFT JOIN FETCH bts.timeSlot ts
    WHERE b.status = :status
      AND b.bookingDate <= :today
    """)
    List<Booking> findApprovedBookingsForAutoCancel(@Param("status") BookingStatus status,
                                                    @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Booking b
           SET b.status = :newStatus,
               b.attendanceStatus = 'NO_SHOW',
               b.updatedBy = :updatedBy,
               b.version = b.version + 1
         WHERE b.id = :id
           AND b.status = 'APPROVED'
           AND b.version = :expectedVersion
    """)
    int atomicCancel(@Param("id") Long id,
                     @Param("newStatus") BookingStatus newStatus,
                     @Param("updatedBy") String updatedBy,
                     @Param("expectedVersion") Integer expectedVersion);

    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE Booking b
       SET b.status = :newStatus,
           b.attendanceStatus = 'CANCELLED',
           b.cancelledBy = :cancelledBy,
           b.updatedBy = :updatedBy,
           b.version = b.version + 1
     WHERE b.id = :id
       AND b.version = :expectedVersion
       AND b.status <> 'CANCELLED'
    """)
    int atomicCancelByStudent(@Param("id") Long id,
                              @Param("newStatus") BookingStatus newStatus,
                              @Param("cancelledBy") String cancelledBy,
                              @Param("updatedBy") String updatedBy,
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
        AND b.status = 'CANCELLED'
        AND b.updatedAt >= :startOfDay
        """)
    Long countCancelledBookingsByUserToday(@Param("userId") Long userId, @Param("startOfDay") Instant startOfDay);

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

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.deletedAt IS NULL GROUP BY b.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate = :today AND b.deletedAt IS NULL")
    long countByBookingDate(@Param("today") LocalDate today);

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.bookingDate = :date AND b.deletedAt IS NULL GROUP BY b.status")
    List<Object[]> countByDateGroupedByStatus(@Param("date") LocalDate date);

    // ── Recommendation & Dashboard enrichment ──────────────────────────────

    @Query("""
            SELECT b.classroom.id,
                   COUNT(b.id),
                   AVG(CAST(COALESCE(b.actualAttendees, b.attendees) AS double))
            FROM Booking b
            WHERE b.user.id = :userId
              AND b.status IN :statuses
              AND b.deletedAt IS NULL
            GROUP BY b.classroom.id
            ORDER BY COUNT(b.id) DESC
            """)
    List<Object[]> findTopClassroomsByUser(
            @Param("userId") Long userId,
            @Param("statuses") List<BookingStatus> statuses,
            Pageable pageable);

    @Query("""
            SELECT bts.timeSlot.id, COUNT(bts.id)
            FROM Booking b JOIN b.bookingTimeSlots bts
            WHERE b.user.id = :userId
              AND b.status IN :statuses
              AND b.deletedAt IS NULL
            GROUP BY bts.timeSlot.id
            ORDER BY COUNT(bts.id) DESC
            """)
    List<Object[]> findTopTimeSlotsByUser(
            @Param("userId") Long userId,
            @Param("statuses") List<BookingStatus> statuses,
            Pageable pageable);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.attendanceStatus = :status AND b.deletedAt IS NULL")
    long countByUserIdAndAttendanceStatus(
            @Param("userId") Long userId,
            @Param("status") AttendanceStatus status);

    @Query("SELECT AVG(CAST(b.actualAttendees AS double)) FROM Booking b WHERE b.user.id = :userId AND b.status = 'COMPLETED' AND b.actualAttendees IS NOT NULL AND b.deletedAt IS NULL")
    Double avgActualAttendeesByUser(@Param("userId") Long userId);

    @Query("""
        SELECT b.classroom.id, COUNT(DISTINCT b.id), COUNT(bts.id), AVG(CAST(COALESCE(b.actualAttendees, b.attendees) AS double))
        FROM Booking b JOIN b.bookingTimeSlots bts
        WHERE b.bookingDate BETWEEN :from AND :to
          AND b.status IN :statuses
          AND b.deletedAt IS NULL
        GROUP BY b.classroom.id
    """)
    List<Object[]> countSlotsByClassroomForRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") List<BookingStatus> statuses);
}
