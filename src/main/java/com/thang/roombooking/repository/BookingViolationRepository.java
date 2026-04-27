package com.thang.roombooking.repository;

import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.PenaltyRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingViolationRepository extends JpaRepository<BookingViolation, Long> {

    List<BookingViolation> findByUserIdAndPenaltyIsNullAndCreatedAtAfter(Long userId, Instant after);

    Page<BookingViolation> findAllByUserId(Long userId, Pageable pageable);

    /**
     * Find all violations that were linked to the given penalty record.
     * Used during penalty revoke to soft-delete them and reset the user's point slate.
     */
    List<BookingViolation> findByPenalty(PenaltyRecord penalty);

    @Query("""
        SELECT v FROM BookingViolation v
        JOIN FETCH v.user u
        WHERE v.deletedAt IS NULL
        ORDER BY v.createdAt DESC
    """)
    List<BookingViolation> findRecentViolations(Pageable pageable);

    @Query("""
        SELECT v.type, COUNT(v), SUM(v.severityPoints)
        FROM BookingViolation v
        WHERE v.createdAt >= :from AND v.createdAt < :to
        GROUP BY v.type
    """)
    List<Object[]> countByDateRangeGroupedByType(
            @Param("from") Instant from,
            @Param("to") Instant to);
}
