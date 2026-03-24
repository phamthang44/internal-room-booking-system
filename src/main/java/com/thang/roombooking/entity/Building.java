package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import lombok.*;

@Builder
@Getter
@Setter
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

    @Override
    public Long getId() {
        return id;
    }
}
