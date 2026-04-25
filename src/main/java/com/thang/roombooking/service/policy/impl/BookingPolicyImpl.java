package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.utils.DateUtils;
import com.thang.roombooking.common.event.ViolationCreatedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.infrastructure.configuration.PenaltyProperties;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.service.policy.BookingPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;
import java.util.Map;

import static com.thang.roombooking.common.constant.SystemConstant.SYSTEM_REGION_TIMEZONE;
import static com.thang.roombooking.common.constant.TimeConstant.CLOSING_TIME;
import static com.thang.roombooking.common.constant.TimeConstant.OPENING_TIME;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingPolicyImpl implements BookingPolicy {
    private final BookingRepository bookingRepository;
    private final PenaltyRecordRepository penaltyRecordRepository;
    private final BookingViolationRepository violationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PenaltyProperties penaltyProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void validatePenalty(Long userId) {
        List<PenaltyRecord> activePenalties = penaltyRecordRepository.findByUserIdAndIsActiveTrue(userId);
        Instant now = Instant.now();

        for (PenaltyRecord penalty : activePenalties) {
            // Check for expiration even if isActive is true (safety check)
            if (penalty.getEndDate() != null && penalty.getEndDate().isBefore(now)) {
                continue;
            }

            if (penalty.getPenaltyAction() == PenaltyAction.BAN_TEMP ||
                penalty.getPenaltyAction() == PenaltyAction.PERMANENT_BAN ||
                penalty.getPenaltyAction() == PenaltyAction.REQUIRE_APPROVAL) {

                String reason = penalty.getReason();
                String translatedReason = translateReason(reason);
                String translatedAction = I18nUtils.get("penalty.action." + penalty.getPenaltyAction().name().toLowerCase());

                String startDateStr = DateUtils.formatDateToString(penalty.getStartDate());
                String endDateStr = penalty.getEndDate() != null ? DateUtils.formatDateToString(penalty.getEndDate()) : "N/A";

                // Return suspension error with detailed info (Action, Reason, Start, End)
                throw new AppException(BookingErrorCode.USER_SUSPENDED, translatedAction, translatedReason, startDateStr, endDateStr);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String translateReason(String reason) {
        if (reason == null || !reason.startsWith("{")) {
            return reason;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(reason, Map.class);
            String key = (String) map.get("key");
            List<Object> args = (List<Object>) map.get("args");
            if (key != null) {
                return I18nUtils.get(key, args != null ? args.toArray() : new Object[0]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse/translate penalty reason: {}", reason);
        }
        return reason;
    }

    // @Override
    // public void validatePenalty(Long userId) {
    //     List<PenaltyRecord> penaltyRecords = penaltyRecordRepository.findByUserIdAndIsActiveTrue(userId);
    //     for (PenaltyRecord penaltyRecord : penaltyRecords) {
    //         if (penaltyRecord.getPenaltyAction() == PenaltyAction.BAN_TEMP || penaltyRecord.getPenaltyAction() == PenaltyAction.PERMANENT_BAN) {
    //             throw new AppException(BookingErrorCode.USER_SUSPENDED,
    //                     penaltyRecord.getReason(),
    //                     penaltyRecord.getStartDate(),
    //                     penaltyRecord.getEndDate());
    //         }
    //     }
    // }

    @Override
    public void validateLeadTimePolicy(LocalDate date) {
        if (date.isAfter(LocalDate.now(ZoneId.of(SYSTEM_REGION_TIMEZONE)).plusDays(7))) {
            throw new AppException(BookingErrorCode.BOOKING_DATE_OVER_LIMIT);
        }
    }

    @Override
    public void validateQuotaPolicy(Long userId, LocalDate date, int requestedSlots) {
        // Count number of reserved time-slots (not number of bookings).
        // This prevents users from bypassing quota by creating bookings in other rooms.
        long bookedTodaySlots = bookingRepository.countBookedSlotsByUserAndDateAndStatuses(
                userId,
                date,
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED) // active bookings
        );

        int maxQuota = 2;
        // Quy tắc: tối đa 2 slots (tương đương 4 tiếng) mỗi ngày
        if (bookedTodaySlots + requestedSlots > maxQuota) {
            throw new AppException(BookingErrorCode.BOOKING_QUOTA_EXCEEDED, maxQuota, 1);
        }
    }

    

    @Override
    public void validatePendingQuota(Long userId) {
        Long pendingCount = bookingRepository.countPendingByUser(userId);
        if (pendingCount >= 3) {
            throw new AppException(BookingErrorCode.TOO_MANY_PENDING_BOOKINGS);
        }
    }

    @Override
    public void validateBookingTimeWorkingHours(LocalDate bookingDate, Instant bookingTime) {
        // Chỉ validate giờ làm việc nếu là đặt cho ngày hôm nay
        // Nếu đặt cho tương lai (ngày mai trở đi), bỏ qua check này
        if (!bookingDate.equals(LocalDate.now(ZoneId.of(SYSTEM_REGION_TIMEZONE)))) {
            return;
        }

        // Chuyển Instant sang giờ Việt Nam
        LocalTime now = LocalTime.ofInstant(bookingTime, ZoneId.of(SYSTEM_REGION_TIMEZONE));

        if (now.isBefore(OPENING_TIME) || now.isAfter(CLOSING_TIME)) {
            throw new AppException(BookingErrorCode.BOOKING_OUT_OF_WORKING_HOURS,
                    OPENING_TIME, CLOSING_TIME);
        }
    }

    @Override
    public void validateNoOverlappingActiveBookings(Long userId, LocalDate bookingDate, List<Integer> requestedTimeSlotIds) {
        if (requestedTimeSlotIds == null || requestedTimeSlotIds.isEmpty()) return;

        List<Long> conflictingIds = bookingRepository.findConflictingBookingIds(
                userId,
                bookingDate,
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CHECKED_IN),
                requestedTimeSlotIds
        );

        if (conflictingIds != null && !conflictingIds.isEmpty()) {
            throw new AppException(BookingErrorCode.BOOKING_USER_DAILY_SLOT_CONFLICT, conflictingIds.getFirst());
        }
    }

    @Override
    public void checkCancellationSpam(Booking booking) {
        Instant startOfDay = LocalDate.now(ZoneId.of(SYSTEM_REGION_TIMEZONE)).atStartOfDay(ZoneId.of(SYSTEM_REGION_TIMEZONE)).toInstant();
        long cancelledToday = bookingRepository.countCancelledBookingsByUserToday(booking.getUser().getId(), startOfDay);
        
        if (cancelledToday >= penaltyProperties.getCancellationSpamThreshold()) {
            log.warn("User {} has excessive cancellations ({}). Logging Violation.", booking.getUser().getId(), cancelledToday);
            BookingViolation violation = BookingViolation.builder()
                    .booking(booking)
                    .reason("booking.cancel.reason.excessive")
                    .user(booking.getUser())
                    .type(ViolationType.EXCESSIVE_CANCELLATION)
                    .resolvedAt(Instant.now())
                    .source(ViolationSource.SYSTEM)
                    .severityPoints(1)
                    .build();
            violationRepository.save(violation);
            eventPublisher.publishEvent(new ViolationCreatedEvent(violation.getId(), violation.getUser().getId()));
        }
    }

    @Override
    public void handleNoShowViolation(Booking booking) {
        log.info("Recording NO_SHOW violation for user {} on booking {}", booking.getUser().getId(), booking.getId());
        
        int points = penaltyProperties.getPoints().getOrDefault(ViolationType.NO_SHOW, 1);
        
        BookingViolation violation = BookingViolation.builder()
                .booking(booking)
                .reason("booking.cancel.reason.no_show")
                .user(booking.getUser())
                .type(ViolationType.NO_SHOW)
                .resolvedAt(Instant.now())
                .source(ViolationSource.SYSTEM)
                .severityPoints(points)
                .build();
                
        violationRepository.save(violation);
        eventPublisher.publishEvent(new ViolationCreatedEvent(violation.getId(), violation.getUser().getId()));
    }
}
