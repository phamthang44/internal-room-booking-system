package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseAuditEntity;
import com.thang.roombooking.common.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_assets")
public class RoomAsset extends BaseAuditEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "asset_type", length = 50)
    @Enumerated(EnumType.STRING)
    private AssetType assetType; // e.g., 'IMAGE', 'BLUEPRINT', '360_VIEW'

    @Builder.Default
    @Column(name = "is_primary")
    private Boolean isPrimary = Boolean.FALSE; // Ảnh đại diện chính cho phòng

}