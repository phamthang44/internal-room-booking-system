package com.thang.roombooking.entity;

public class ClassroomAnalytic {

    private Long id;

    private Classroom classroom;

    private int totalCheckIns;

    private int popularityScore;

    //Dùng chính cái BookingStatusChangedEvent mà Thắng vừa làm. Khi có event CHECK_IN thành công, Listener sẽ cộng +1 vào total_checkins cho phòng đó.

}
