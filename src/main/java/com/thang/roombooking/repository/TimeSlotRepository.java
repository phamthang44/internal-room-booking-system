package com.thang.roombooking.repository;

import com.thang.roombooking.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot,Integer> {

    @Query("SELECT t FROM TimeSlot t WHERE t.startTime <= :time AND t.endTime >= :time")
    Optional<TimeSlot> findSlotByTime(@Param("time") LocalTime time);

    Optional<TimeSlot> findFirstByStartTimeGreaterThanEqualOrderByStartTimeAsc(LocalTime time);

    Optional<TimeSlot> findFirstByOrderByStartTimeAsc();
}
