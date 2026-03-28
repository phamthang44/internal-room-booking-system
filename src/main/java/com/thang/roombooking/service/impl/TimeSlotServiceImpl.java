package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.entity.TimeSlot;
import com.thang.roombooking.repository.TimeSlotRepository;
import com.thang.roombooking.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    @Override
    public TimeSlot getTimeSlotById(int id) {
        return timeSlotRepository.findById(id).orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Time slot ID: " + id));
    }

    @Override
    public List<TimeSlot> getTimeSlotsByIds(List<Integer> ids) {
        return timeSlotRepository.findAllById(ids);
    }
}
