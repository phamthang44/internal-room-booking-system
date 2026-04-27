package com.thang.roombooking.repository;

import com.thang.roombooking.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    long countByIdIn(List<Integer> ids);

    List<Equipment> findAllByIdIn(List<Integer> ids);

    // Paginated list with optional nameKey keyword search
    @Query(value = """
        SELECT e.id, e.name_key, e.description_key, e.deleted_at,
               COUNT(ce.classroom_id) AS classroom_count
        FROM equipments e
        LEFT JOIN classroom_equipment ce ON ce.equipment_id = e.id
        LEFT JOIN classrooms c ON c.id = ce.classroom_id AND c.deleted_at IS NULL
        WHERE e.deleted_at IS NULL
          AND (CAST(:keyword AS text) IS NULL OR e.name_key ILIKE '%' || :keyword || '%')
        GROUP BY e.id
        ORDER BY e.name_key ASC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findAllForAdmin(@Param("keyword") String keyword,
                                   @Param("size") int size,
                                   @Param("offset") long offset);

    @Query(value = """
        SELECT COUNT(DISTINCT e.id)
        FROM equipments e
        WHERE e.deleted_at IS NULL
          AND (CAST(:keyword AS text) IS NULL OR e.name_key ILIKE '%' || :keyword || '%')
        """, nativeQuery = true)
    long countForAdmin(@Param("keyword") String keyword);

    // Deactivation guard: true if equipment is used by any non-deleted classroom
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM classroom_equipment ce
            JOIN classrooms c ON c.id = ce.classroom_id
            WHERE ce.equipment_id = :id
              AND c.deleted_at IS NULL
        )
        """, nativeQuery = true)
    boolean isAssignedToAnyClassroom(@Param("id") Integer id);

    // nameKey uniqueness check (for create)
    boolean existsByNameKey(String nameKey);

    // Reactivation: find including soft-deleted rows (bypasses @SQLRestriction)
    @Query(value = "SELECT * FROM equipments WHERE id = :id", nativeQuery = true)
    Optional<Equipment> findByIdIncludingDeleted(@Param("id") Integer id);

    // Count non-deleted classrooms that use this equipment (for detail response)
    @Query(value = """
        SELECT COUNT(*)
        FROM classroom_equipment ce
        JOIN classrooms c ON c.id = ce.classroom_id
        WHERE ce.equipment_id = :id
          AND c.deleted_at IS NULL
        """, nativeQuery = true)
    long countClassroomsForEquipment(@Param("id") Integer id);

    // Reactivation: clear deleted_at
    @Modifying
    @Query(value = "UPDATE equipments SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void reactivateById(@Param("id") Integer id);
}
