package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.AssetType;
import com.thang.roombooking.common.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE public.classrooms SET deleted_at = NOW(), room_name = CONCAT(room_name, '-deleted-', id) WHERE id = ?")
@Builder
@Entity
@Table(name = "classrooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"building_id", "room_name"})
})
public class Classroom extends BaseSoftDeleteEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Converted to LAZY to avoid N+1 issues when fetching large number of classrooms
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "room_name", nullable = false, length = 20)
    private String roomName;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
    //dùng unidirectional hiệu quả hơn vì hiếm khi hỏi "Loại phòng lab này cần những phòng nào ?"
    // case : search màn hình select drop down gì đó lọc thông qua id chọn room repository load lên
    // bidirectional sẽ khiến nó có khả năng bị mem-leak nếu room type có 1000 object rooms, Infinite Recursion
    //dirty check, hibernate track theo dõi 1 lúc 2 thằng nên flush sẽ chậm đi

    @Builder.Default
    @OneToMany(mappedBy = "classroom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomAsset> roomAssets = new ArrayList<>();

    public void addAsset(RoomAsset asset) {
        roomAssets.add(asset);
        asset.setClassroom(this); // Sync 2 chiều trong bộ nhớ
    }

    public void removeAsset(RoomAsset asset) {
        roomAssets.remove(asset);
        asset.setClassroom(null);
    }

    public void updateDetails(Building building, RoomType roomType, Map<Equipment, Integer> equipmentMap, RoomStatus status, List<String> imageUrls) {
        this.building = building;
        this.roomType = roomType;
        this.status = status;

        // 1. Loại bỏ những thiết bị không còn nằm trong Map mới
        this.classroomEquipments.removeIf(existing -> !equipmentMap.containsKey(existing.getEquipment()));

        // 2. Thêm hoặc Cập nhật số lượng cho những thiết bị trong Map
        updateEquipments(equipmentMap);

        // Diff cho Room Assets (Ảnh)
        this.updateAssets(imageUrls);
    }

    private void updateEquipments(Map<Equipment, Integer> equipmentMap) {
        equipmentMap.forEach((equipment, quantity) -> {
            // Tìm xem thiết bị này đã có trong phòng chưa
            this.classroomEquipments.stream()
                    .filter(ce -> ce.getEquipment().equals(equipment))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.setQuantity(quantity), // Nếu có rồi thì chỉ cập nhật số lượng
                            () -> { // Nếu chưa có thì mới thêm mới
                                ClassroomEquipment newCe = ClassroomEquipment.builder()
                                        .id(new ClassroomEquipmentId(this.id, equipment.getId()))
                                        .classroom(this)
                                        .equipment(equipment)
                                        .quantity(quantity)
                                        .build();
                                this.classroomEquipments.add(newCe);
                            }
                    );
        });
    }

    public void updateAssets(List<String> newUrls) {
        if (newUrls == null) {
            this.roomAssets.clear();
            return;
        }

        // 1. Xóa các ảnh cũ không còn nằm trong danh sách URL mới
        this.roomAssets.removeIf(asset -> !newUrls.contains(asset.getUrl()));

        // 2. Lấy danh sách các URL hiện đang có trong Entity để so sánh
        Set<String> existingUrls = this.roomAssets.stream()
                .map(RoomAsset::getUrl)
                .collect(Collectors.toSet());

        // 3. Chỉ thêm những URL mới chưa tồn tại
        newUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .forEach(url -> {
                    RoomAsset newAsset = RoomAsset.builder()
                            .url(url)
                            .assetType(AssetType.IMAGE)
                            .isPrimary(false)
                            .build();
                    this.addAsset(newAsset); // Sử dụng helper method để sync 2 chiều
                });
    }


    @Override
    public Long getId() {
        return id;
    }
}
