package com.thang.roombooking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class ClassroomEquipmentId implements Serializable {

    @Column(name = "classroom_id")
    private Long classroomId;

    @Column(name = "equipment_id")
    private Integer equipmentId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassroomEquipmentId)) return false;
        ClassroomEquipmentId that = (ClassroomEquipmentId) o;
        return Objects.equals(classroomId, that.classroomId) &&
               Objects.equals(equipmentId, that.equipmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classroomId, equipmentId);
    }
}
