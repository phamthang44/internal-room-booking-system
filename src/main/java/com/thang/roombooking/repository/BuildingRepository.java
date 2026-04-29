package com.thang.roombooking.repository;

import com.thang.roombooking.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
    boolean existsById(Long id);

    Optional<Building> findById(Long id);

    boolean existsByNameKey(String nameKey);

    boolean existsByNameKeyAndIdNot(String nameKey, Long id);

    List<Building> findAllByOrderByIdAsc();
}
