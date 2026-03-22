package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.RoomStatus;
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
@Table(name = "classrooms")
public class Classroom extends BaseSoftDeleteEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Converted to LAZY to avoid N+1 issues when fetching large number of classrooms
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    @Version
    @Column(name = "version")
    private Integer version;

    @Builder.Default
    @OneToMany(mappedBy = "classroom", cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClassroomEquipment> classroomEquipments = new ArrayList<>();

    @Override
    public Long getId() {
        return id;
    }
}
