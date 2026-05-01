package com.thang.roombooking.repository;

import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.Equipment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long>, JpaSpecificationExecutor<Classroom> {
    boolean existsClassroomByRoomName(String roomName);

    boolean existsByBuildingIdAndRoomName(Long buildingId, String roomName);

    @Query("SELECT c.classroomEquipments FROM Classroom c WHERE c.id IN :ids")
    List<Equipment> findAllEquipmentsByClassroomIds(@Param("ids") List<Long> ids);

    @Query(value = """
        SELECT e.* FROM equipments e
        JOIN classroom_equipments ce ON e.id = ce.equipment_id
        WHERE ce.classroom_id IN (:classroomIds)
    """, nativeQuery = true)
    List<Equipment> findEquipmentsByNativeSql(@Param("classroomIds") List<Long> classroomIds);

    boolean existsByBuildingIdAndRoomNameAndIdNot(Long buildingId, String roomName, Long currentId);

    @Query("SELECT COUNT(c) FROM Classroom c WHERE c.status = :status AND c.deletedAt IS NULL")
    long countActiveByStatus(@Param("status") RoomStatus status);

    @Query("""
            SELECT DISTINCT c FROM Classroom c
            JOIN FETCH c.building
            JOIN FETCH c.roomType
            WHERE c.status = 'AVAILABLE'
              AND c.capacity >= :minCapacity
              AND c.deletedAt IS NULL
            """)
    List<Classroom> findAvailableWithMinCapacity(@Param("minCapacity") int minCapacity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Classroom c WHERE c.id = :id")
    Optional<Classroom> findByIdWithLock(@Param("id") Long id);
}
