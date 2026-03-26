package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.entity.TimeSlot;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.BookingCommandService;
import com.thang.roombooking.service.ClassroomQueryService;
import com.thang.roombooking.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingCommandServiceImpl implements BookingCommandService {

    private final BookingRepository bookingRepository;
    private final ClassroomQueryService classroomQueryService;
    private final TimeSlotService timeSlotService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingResponse createBooking(CreateBookingRequest request) {

        // validate classroom information
        classroomQueryService.validateRoomAvailable(request.classroomId(), request.buildingId());

        //validate booking date
        if (request.bookingDate() != null) {
            if (request.bookingDate().isBefore(LocalDate.now())) {
                throw new AppException(BookingErrorCode.BOOKING_DATE_PAST);
            } else if (request.bookingDate().isAfter(LocalDate.now())) {
                if (!request.bookingDate().isAfter(LocalDate.now().plusDays(7))) {
                  throw new AppException(BookingErrorCode.BOOKING_DATE_OVER_AVAILABLE_WEEK_7_DAYS_ALLOWED);
                }
            }
        }

        //Working hours policy
        //only accept 7:00 sáng tới 17:30

        //ko đặt quá 4 tiếng tức tối đa 2 slot liền kề nhau tức only accept quota booking là 2


        // time slot hiện tại ta có mỗi slot 2 tiếng khoảng cách giữa các room sau khi đc use là 30p.

        // tức phải làm sao để báo user sau khi phòng đc sử dụng thì đợi 30p ? or 1h cho chắc ? thế thì ko khớp timeline slot lắm thế thì phải 30 phút
        TimeSlot timeSlot = timeSlotService.getTimeSlotById(request.timeSlotId());
        // thế how about case đầu tiên trong ngày 7h sáng - 9h sáng ?
        // thế rule đặt sẽ khác à ?
        if (request.timeBooking() != null) {
            String rawTimeString = request.timeBooking().toString();
            String timeFormatted = rawTimeString.split("T")[1];
            LocalTime time = LocalTime.parse(timeFormatted, DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (time.isBefore(timeSlot.getStartTime().minusMinutes(30))) {
                throw AppException("Please come back after 30 minutes")
            }
        }





        return null;
    }
}
