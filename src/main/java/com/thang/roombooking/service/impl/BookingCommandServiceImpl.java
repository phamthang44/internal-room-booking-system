package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.BookingApprovalRequest;
import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.repository.BookingApprovalRepository;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.service.*;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingCommandServiceImpl implements BookingCommandService {

    private final BookingRepository bookingRepository;
    private final BookingValidatorService bookingValidatorService;
    private final TimeSlotService timeSlotService;
    private final BookingPolicyManager bookingPolicyManager;
    private final ClassroomRepository classroomRepository;
    private final TranslationService translationService;
    private final BookingMapper bookingMapper;
    private final BookingApprovalCommandService bookingApprovalCommandService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateBookingResponse createBooking(CreateBookingRequest request, UserAccount currentUser) {
        log.info("{} | Create Booking | User: {} | Data: {}",
                LogConstant.ACTION_START, currentUser.getId(), request);
        try {
            // 1. Validate các lớp (Lớp 1 & Lớp 2)
            bookingValidatorService.validateClassroom(request.classroomId(), request.attendees());
            bookingValidatorService.validateBookingDate(request.bookingDate());
            bookingValidatorService.validatePurpose(request.purpose());
            bookingPolicyManager.validateBookingTimeWorkingHours(request.timeBooking());

            // 2. Policy Quota & Penalty
            bookingPolicyManager.validatePenalty(currentUser.getId()); //TODO: tạm thời luôn cho qua chưa tính tới
            bookingPolicyManager.validateQuotaPolicy(currentUser.getId(), request.bookingDate(), request.timeSlotIds().size());

            // 3. Lấy thực thể TimeSlot (Dùng chung một hàm List cho gọn)
            List<TimeSlot> timeSlots = timeSlotService.getTimeSlotsByIds(request.timeSlotIds());

            bookingValidatorService.validateTimeSlots(request.bookingDate(),  timeSlots);
            // 4. Build Entity với nguyên tắc XOR
            Booking booking = Booking.builder()
                    .user(currentUser)
                    .classroom(classroomRepository.getReferenceById(request.classroomId()))
                    .bookingDate(request.bookingDate())
                    .startTime(null)
                    .endTime(null)
                    .purpose(request.purpose())
                    .status(BookingStatus.PENDING)
                    .build();

            // 5. Mapping bảng trung gian (Tránh lỗi Casting của Thắng)
            List<BookingTimeSlot> bookingTimeSlots = timeSlots.stream()
                    .map(slot -> BookingTimeSlot.builder()
                            .booking(booking)
                            .timeSlot(slot)
                            .build())
                    .toList();
            booking.setBookingTimeSlots(bookingTimeSlots);

            // LOG SUCCESS: Xác nhận hoàn tất
            bookingRepository.save(booking);

            log.info("{}: Booking created with ID: {} for User: {}",LogConstant.ACTION_SUCCESS, booking.getId(), currentUser.getId());
            Map<String, String> timeSlotTranslations = translationService.getAllTimeSlotTranslations();
            Map<String, String> buildingTranslations = translationService.getTranslations(getBuildingTranslationIds(booking.getClassroom().getBuilding()));
            Map<String, String> combinedTranslations = new HashMap<>(timeSlotTranslations);
            combinedTranslations.putAll(buildingTranslations);
            return bookingMapper.toCreateBookingResponse(booking, combinedTranslations);
        } catch (AppException e) {
            log.warn("{}: Failed to create booking for User: {}. Reason: {}", LogConstant.BIZ_ERROR,
                    currentUser.getId(), e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{}: Unexpected error during booking creation for User: {}", LogConstant.SYS_ERROR,
                    currentUser.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(CheckInRequest request, UserAccount currentUser) {
        log.info("{} | Check in Booking | User: {} | Data: {}",
                LogConstant.ACTION_START, currentUser.getId(), request);
        try {
            // lấy booking và time slots
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            // Check quyền sở hữu
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            List<TimeSlot> slots = booking.getBookingTimeSlots().stream()
                    .map(BookingTimeSlot::getTimeSlot)
                    .sorted(Comparator.comparing(TimeSlot::getStartTime))
                    .toList();

            // 2. Nhờ Validator tìm Slot hợp lệ
            TimeSlot targetSlot = bookingValidatorService.validateAndGetTargetSlot(slots, booking.getBookingDate(), LocalTime.now());

            // 3. ATOMIC UPDATE: Chặn đứng mọi nỗ lực duplicate request
            int updatedRows = bookingRepository.atomicCheckIn(booking.getId(), booking.getVersion());

            if (updatedRows == 0) {
                // Nếu đã CHECKED_IN rồi thì status không còn là APPROVED -> updatedRows = 0
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_CHECKED_IN);
            }

            log.info("{}: Booking checkin with ID: {} for User: {}",LogConstant.ACTION_SUCCESS, booking.getId(), currentUser.getId());
            //notificationService.sendCheckInSuccess(booking, targetSlot); TODO: future feature notification service
        } catch (AppException e) {
            log.warn("{}: Failed to checkin booking for User: {}. Reason: {}", LogConstant.BIZ_ERROR,
                    currentUser.getId(), e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Unexpected error during booking checkin for User: {}", LogConstant.SYS_ERROR,
                    currentUser.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBooking(BookingApprovalRequest request, UserAccount currentUser) {
        log.info("{}: Booking approve with ID: {} by STAFF: {}",LogConstant.ACTION_SUCCESS, request.bookingId(), currentUser.getId());
        try {
            // lấy booking
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
            if (booking.getStatus() != BookingStatus.PENDING) {
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }
            // 3. Thực hiện Atomic Update (Kết hợp Optimistic Locking)
            // Truyền booking.getVersion() vào để DB đối chiếu
            int updatedRows = bookingRepository.atomicApprove(
                    booking.getId(),
                    BookingStatus.APPROVED,
                    booking.getVersion()
            );
            if (updatedRows == 0) {
                // Nếu trả về 0, nghĩa là giữa lúc Select và Update đã có Admin khác nhanh tay hơn
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }

            log.info("{} | Booking ID: {} approved successfully", LogConstant.ACTION_SUCCESS, request.bookingId());
            bookingApprovalCommandService.saveApprovalBooking(booking, currentUser);

            // TODO: Gửi RabbitMQ/WebSocket tại đây

        } catch (AppException e) {
            log.warn("{}: Failed to approve | Reason: {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("{} | Conflict Error |", LogConstant.SYS_ERROR, e.getCause());
            throw e;
        }
        catch (Exception e) {
            log.error("{} | Unexpected System Error |", LogConstant.SYS_ERROR, e);
            throw e;
        }
        //notificationService.sendBookingApproveSuccess(booking, targetSlot); TODO: future feature notification service
    }

    private Map<TranslatableEntityType, Set<Long>> getBuildingTranslationIds(Building building) {
        if (building == null) return Collections.emptyMap();

        Map<TranslatableEntityType, Set<Long>> idsByType = new HashMap<>();
        // Ép kiểu ID từ Integer/Long sang Set<Long> để khớp với tham số của Service
        idsByType.put(TranslatableEntityType.BUILDING, Set.of(building.getId()));

        return idsByType;
    }
}
