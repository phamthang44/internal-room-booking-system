package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Builder
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE public.building SET deleted_at = NOW(), name_key = CONCAT(name_key, '-deleted-', id) WHERE id = ?")
@Entity
@Table(name = "buildings")
@NoArgsConstructor
@AllArgsConstructor
public class Building extends BaseSoftDeleteEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_key", length = 100)
    private String nameKey;

    @Column(length = 255)
    private String address;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

}
