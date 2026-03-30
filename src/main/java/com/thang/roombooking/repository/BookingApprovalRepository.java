package com.thang.roombooking.repository;

import com.thang.roombooking.entity.BookingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingApprovalRepository extends JpaRepository<BookingApproval, Long> {

}
