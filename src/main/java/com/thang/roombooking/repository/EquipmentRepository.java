package com.thang.roombooking.repository;

import com.thang.roombooking.entity.Equipment;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    long countByIdIn(List<Integer> ids);

    List<Equipment> findAllByIdIn(List<Integer> ids);
}
