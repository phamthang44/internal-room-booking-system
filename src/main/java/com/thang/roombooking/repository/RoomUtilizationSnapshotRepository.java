package com.thang.roombooking.repository;

import com.thang.roombooking.entity.RoomUtilizationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomUtilizationSnapshotRepository extends JpaRepository<RoomUtilizationSnapshot, Long> {

    Optional<RoomUtilizationSnapshot> findByClassroomIdAndWeekStart(Long classroomId, LocalDate weekStart);

    List<RoomUtilizationSnapshot> findByWeekStartBetweenOrderByUtilizationPctDesc(LocalDate from, LocalDate to);
}
