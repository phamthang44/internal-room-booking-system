package com.thang.roombooking.service;

import com.thang.roombooking.entity.TimeSlot;

import java.util.List;

public interface TimeSlotService {

    TimeSlot getTimeSlotById(int id);
    List<TimeSlot> getTimeSlotsByIds(List<Integer> ids);

}
