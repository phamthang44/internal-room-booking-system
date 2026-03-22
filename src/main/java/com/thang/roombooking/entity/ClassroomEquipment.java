package com.thang.roombooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_equipment")
public class ClassroomEquipment {

    @EmbeddedId
    private ClassroomEquipmentId id;

    // Using LAZY fetch type to prevent N+1 query problems. Best practice for ManyToOne.
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classroomId")
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    // EAGER might seem tempting here, but LAZY provides better performance control. 
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("equipmentId")
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "quantity", columnDefinition = "INT DEFAULT 1")
    @Builder.Default
    private Integer quantity = 1;
}
