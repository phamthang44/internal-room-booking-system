package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.utils.TextValidationUtils;
import com.thang.roombooking.entity.TimeSlot;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.BookingValidatorService;
import com.thang.roombooking.service.ClassroomValidatorService;
import com.thang.roombooking.service.TimeSlotService;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingValidatorServiceImpl implements BookingValidatorService {

    private final BookingRepository bookingRepository;
    private final ClassroomValidatorService classroomValidatorService;
    private final BookingPolicyManager bookingPolicyManager;
    private final TimeSlotService timeSlotService;

    @Override
    public void validateBookingDate(LocalDate date) {
        if (date == null) return;

        // Rule 1: Không được đặt ngày quá khứ
        if (date.isBefore(LocalDate.now())) {
            throw new AppException(BookingErrorCode.BOOKING_DATE_IN_PAST);
        }

        // Rule 2: Chỉ được đặt trong vòng 7 ngày tới
        // Logic đúng: Nếu date sau (now + 7 ngày) thì ném lỗi
        bookingPolicyManager.validateLeadTimePolicy(date);

    }


    @Override
    public void validatePurpose(String purpose) {
        String cleanPurpose = TextValidationUtils.sanitize(purpose);
        if (TextValidationUtils.containsHtmlTags(cleanPurpose)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "html tags are not allowed in purpose");
        }
        if (TextValidationUtils.containsScriptTags(cleanPurpose)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "script tags are not allowed in purpose");
        }
        if (TextValidationUtils.containsSqlInjection(cleanPurpose)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "SQL injection are not allowed in purpose");
        }
        if (TextValidationUtils.containsBadWords(cleanPurpose)) throw new AppException(CommonErrorCode.INVALID_REQUEST, "bad words");
    }

    @Override
    public void validateTimeSlot(int timeSlotId) {
        TimeSlot timeSlot = timeSlotService.getTimeSlotById(timeSlotId);
        if (timeSlot == null) throw new AppException(CommonErrorCode.INVALID_REQUEST, "");
    }

    @Override
    public void validateClassroom(Long classroomId, int attendees) {
        //check xem phòng còn sống ko ?
        classroomValidatorService.validateRoomStatus(classroomId);
        //check chỗ ngồi
        classroomValidatorService.validateRoomCapacity(classroomId, attendees);
    }

    public void validateTimeSlots(LocalDate bookingDate, List<TimeSlot> selectedSlots) {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        if (bookingDate.equals(today)) {
            for (TimeSlot slot : selectedSlots) {
                // Nếu giờ kết thúc của Slot đã qua so với giờ hiện tại
                if (slot.getEndTime().isBefore(now)) {
                    throw new AppException(BookingErrorCode.TIMESLOT_ALREADY_ENDED);
                }
            }
        }
    }

    @Override
    public TimeSlot validateAndGetTargetSlot(List<TimeSlot> slots, LocalDate bookingDate, LocalTime now) {
        LocalDate today = LocalDate.now();

        // 1. Chặn nếu không phải ngày hôm nay
        if (bookingDate.isAfter(today)) {
            long daysDiff = ChronoUnit.DAYS.between(today, bookingDate);
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_TOO_EARLY_DAYS, daysDiff);
        }
        if (bookingDate.isBefore(today)) {
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_EXPIRED_DAYS);
        }

        // 2. Nếu đã đúng là ngày hôm nay, mới thực hiện logic tìm Slot và so sánh Giờ
        TimeSlot closestSlot = slots.stream()
                .min(Comparator.comparingLong(s ->
                        Math.abs(Duration.between(s.getStartTime(), now).toMinutes())))
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

        long diffMinutes = Duration.between(closestSlot.getStartTime(), now).toMinutes();

        if (diffMinutes < -15) {
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_TOO_EARLY, diffMinutes);
        }

        if (diffMinutes > 15) {
            throw new AppException(BookingErrorCode.BOOKING_CHECK_IN_EXPIRED, Math.abs(diffMinutes));
        }

        return closestSlot;
    }
}
