package com.thang.roombooking.repository;

import com.thang.roombooking.entity.BookingHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory,Long> {

    @Query("""
    SELECT h\s
    FROM BookingHistory h
    JOIN FETCH h.booking b
    JOIN FETCH b.classroom c
    WHERE b.user.id = :userId
    ORDER BY h.createdAt DESC
   \s""")
    List<BookingHistory> findRecentActivities(Long userId, Pageable pageable);

    List<BookingHistory> findByBookingId(Long bookingId);
}
