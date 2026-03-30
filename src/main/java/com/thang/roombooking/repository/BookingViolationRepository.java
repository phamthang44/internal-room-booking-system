package com.thang.roombooking.repository;

import com.thang.roombooking.entity.BookingViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingViolationRepository extends JpaRepository<BookingViolation, Long> {
}
