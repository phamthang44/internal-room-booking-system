package com.thang.roombooking.repository;

import com.thang.roombooking.entity.BookingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingApprovalRepository extends JpaRepository<BookingApproval, Long> {

    /**
     * Returns all approval records for a given booking, ordered by creation time (oldest first).
     * Used to build the audit/approval-history trail in {@link BookingDetailResponse}.
     */
    List<BookingApproval> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    /** Convenience alias used by BookingQueryServiceImpl. */
    default List<BookingApproval> findByBookingId(Long bookingId) {
        return findByBookingIdOrderByCreatedAtAsc(bookingId);
    }
}
