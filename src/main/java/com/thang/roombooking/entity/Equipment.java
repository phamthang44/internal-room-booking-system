package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "equipments")
public class Equipment extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name_key", length = 100, nullable = false, unique = true)
    private String nameKey;

    @Column(name = "description_key", length = 255)
    private String descriptionKey;

    // OneToMany mapping back to the join entity
    @Builder.Default
    @OneToMany(mappedBy = "equipment", cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private List<ClassroomEquipment> classroomEquipments = new ArrayList<>();
}
