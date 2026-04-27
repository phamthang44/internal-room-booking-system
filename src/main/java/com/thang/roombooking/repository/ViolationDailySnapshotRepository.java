package com.thang.roombooking.repository;

import com.thang.roombooking.common.enums.ViolationType;
import com.thang.roombooking.entity.ViolationDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViolationDailySnapshotRepository extends JpaRepository<ViolationDailySnapshot, Long> {

    List<ViolationDailySnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate from, LocalDate to);

    Optional<ViolationDailySnapshot> findBySnapshotDateAndViolationType(LocalDate date, ViolationType type);
}
