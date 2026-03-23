package com.thang.roombooking.entity;

import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

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


    @Override
    public Long getId() {
        return id;
    }
}
