package com.thang.roombooking.repository;

import com.thang.roombooking.entity.DashboardDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardDailySnapshotRepository extends JpaRepository<DashboardDailySnapshot, Long> {

    Optional<DashboardDailySnapshot> findBySnapshotDate(LocalDate date);

    List<DashboardDailySnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate from, LocalDate to);
}
