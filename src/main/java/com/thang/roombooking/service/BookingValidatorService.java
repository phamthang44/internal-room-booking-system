package com.thang.roombooking.service;

import com.thang.roombooking.entity.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingValidatorService {

    void validateBookingDate(LocalDate date);

    void validatePurpose(String purpose);

    void validateTimeSlot(int timeSlotId);

    void validateClassroom(Long classroomId, int attendees);

    void validateTimeSlots(LocalDate bookingDate, List<TimeSlot> selectedSlots);

    TimeSlot validateAndGetTargetSlot(List<TimeSlot> slots, LocalDate bookingDate, LocalTime now);

}
