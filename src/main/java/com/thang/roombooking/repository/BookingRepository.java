package com.thang.roombooking.repository;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByClassroomIdAndEndTimeAfter(Long roomId, Instant now);

    boolean existsByClassroomIdAndStatusInAndStartTimeAfter(
            Long classroomId,
            List<BookingStatus> statuses,
            Instant time
    );

    boolean existsByClassroomIdAndStatusInAndEndTimeAfter(Long roomId, List<BookingStatus> approved, Instant now);
}
