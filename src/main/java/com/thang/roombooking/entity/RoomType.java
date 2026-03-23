package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseAuditEntity;
import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_types")
public class RoomType extends BaseSoftDeleteEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_key", nullable = false, unique = true)
    private String nameKey; // Dùng để map với bảng Translation (vd: 'room_type.lecture_hall')

    @Column(name = "description_key")
    private String descriptionKey;
}