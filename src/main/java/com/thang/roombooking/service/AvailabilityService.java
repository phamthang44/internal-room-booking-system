package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.ClassroomAvailabilityResponse;
import com.thang.roombooking.common.dto.response.DateAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AvailabilityService {

    /**
     * Lấy trạng thái trống của 1 phòng trong 1 khoảng ngày (thường là 7 ngày tới)
     */
    ClassroomAvailabilityResponse getClassroomAvailability(Long classroomId, LocalDate startDate, LocalDate endDate);

    /**
     * Efficiently lookup availability metadata for multiple classrooms over a specific date.
     * Prevents database N+1 loop issues when parsing public paginated listings.
     */
    Map<Long, DateAvailability> getBulkClassroomsAvailabilityForDate(List<Long> classroomIds, LocalDate date);

    /**
     * Gợi ý các phòng trống nhanh dựa trên tiêu chí (Số lượng người, thời gian)
     * Đây là nơi Thắng áp dụng "Scoring" để đưa phòng tốt nhất lên đầu
     */
    //List<ClassroomSummaryResponse> findRecommendedClassrooms(int attendees, LocalDateTime preferredTime);


}
