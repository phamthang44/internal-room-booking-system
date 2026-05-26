package com.thang.roombooking.fixture;

import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.Classroom;

public final class ClassroomFixtures {

    private ClassroomFixtures() {}

    public static Building building() {
        return Building.builder()
                .id(5L)
                .build();
    }

    public static Classroom available() {
        return Classroom.builder()
                .id(10L)
                .roomName("A101")
                .capacity(50)
                .status(RoomStatus.AVAILABLE)
                .building(building())
                .version(0)
                .build();
    }

    public static Classroom inactive() {
        return Classroom.builder()
                .id(11L)
                .roomName("B202")
                .capacity(30)
                .status(RoomStatus.INACTIVE)
                .building(building())
                .version(0)
                .build();
    }
}
