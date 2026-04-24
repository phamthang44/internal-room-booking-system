package com.thang.roombooking.repository;

import com.thang.roombooking.entity.BookingViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingViolationRepository extends JpaRepository<BookingViolation, Long> {

    List<BookingViolation> findByUserIdAndCreatedAtAfter(Long userId, Instant after);

    Page<BookingViolation> findAllByUserId(Long userId, Pageable pageable);
}
