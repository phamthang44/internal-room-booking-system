package com.thang.roombooking.repository;

import com.thang.roombooking.entity.PenaltyRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRecordRepository extends JpaRepository<PenaltyRecord, Long> {
    
    List<PenaltyRecord> findByUserIdAndIsActiveTrue(Long userId);
    Page<PenaltyRecord> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT p FROM PenaltyRecord p WHERE p.isActive = true AND p.endDate IS NOT NULL AND p.endDate < :now")
    List<PenaltyRecord> findExpiredPenalties(@org.springframework.data.repository.query.Param("now") java.time.Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PenaltyRecord p SET p.isActive = false, p.updatedAt = :now WHERE p.isActive = true AND p.endDate IS NOT NULL AND p.endDate < :now")
    int deactivateExpiredPenalties(@org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
