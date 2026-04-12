package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.BookingApprovalRequest;
import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.enums.BookingAction;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.enums.ViolationType;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BookingApprovalRepository;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.service.*;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
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
    private final BookingViolationRepository violationRepository;
    private final ApplicationEventPublisher eventPublisher;

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

            LocalTime bookingStartTime = timeSlots.stream()
                    .map(TimeSlot::getStartTime)
                    .min(LocalTime::compareTo)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
            
            LocalTime bookingEndTime = timeSlots.stream()
                    .map(TimeSlot::getEndTime)
                    .max(LocalTime::compareTo)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            // 4. Build Entity với nguyên tắc XOR
            Booking booking = Booking.builder()
                    .user(currentUser)
                    .classroom(classroomRepository.getReferenceById(request.classroomId()))
                    .bookingDate(request.bookingDate())
                    .startTime(bookingStartTime)
                    .endTime(bookingEndTime)
                    .attendees(request.attendees())
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
            Booking savedBooking = bookingRepository.save(booking);

            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    savedBooking,
                    BookingStatus.PENDING,
                    BookingAction.CREATE_BOOKING.name(),
                    currentUser.getEmail(),
                    "Khởi tạo đặt phòng"
            ));

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
            bookingPolicyManager.validateCheckInTimePolicy(request.checkInTime());
            // lấy booking và time slots
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateCheckInStatus(booking.getStatus());

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

            if (updatedRows > 0) {
                syncBookingState(booking, BookingStatus.CHECKED_IN);

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        booking,
                        BookingStatus.CHECKED_IN,
                        BookingAction.CHECK_IN.name(),
                        currentUser.getEmail(),
                        "Sinh viên điểm danh thành công"
                ));
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
    public Long approveBooking(BookingApprovalRequest request, UserAccount currentUser) {
        log.info("{}: Booking approve with ID: {} by STAFF: {}",LogConstant.ACTION_SUCCESS, request.bookingId(), currentUser.getId());
        try {
            // lấy booking
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateApproveStatus(booking.getStatus());

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

            Booking approvedBooking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            log.info("{} | Booking ID: {} approved successfully", LogConstant.ACTION_SUCCESS, request.bookingId());
            bookingApprovalCommandService.saveApprovalBooking(approvedBooking, currentUser);
            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    approvedBooking,
                    BookingStatus.APPROVED,
                    BookingAction.APPROVE_BOOKING.name(),
                    currentUser.getEmail(),
                    "Staff/Admin đã chấp nhận đơn đặt phòng"
            ));
            // TODO: Gửi RabbitMQ/WebSocket tại đây
            return approvedBooking.getId();
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

    @Transactional
    @Override
    public void cancelExpiredBooking(Booking booking) {
        // Atomic Update: Chỉ hủy nếu status vẫn đang là APPROVED
        int updatedRows = bookingRepository.atomicCancel(
                booking.getId(),
                BookingStatus.CANCELLED,
                booking.getVersion()
        );

        if (updatedRows > 0) {
            syncBookingState(booking, BookingStatus.CANCELLED);

            // Ghi nhận vi phạm vào bảng booking_violations để sau này xử phạt (Penalty)
            BookingViolation violation = BookingViolation.builder()
                    .booking(booking)
                    .reason("booking.cancel.reason.no_show")
                    .user(booking.getUser())
                    .type(ViolationType.NO_SHOW)
                    .resolvedAt(Instant.now())
                    .build();
            violationRepository.save(violation);
            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    booking,
                    BookingStatus.CANCELLED,
                    BookingAction.CANCEL_BOOKING.name(),
                    "SYSTEM",
                    "booking.cancel.reason.no_show"
            ));
            // TODO: Bắn notification báo cho sinh viên là đơn đã bị hủy do đi muộn
        }
    }

    @Transactional
    @Override
    public void autoRejectOverduePendingBooking(Booking booking) {
        // Atomic Update: Chỉ từ chối nếu status vẫn đang là PENDING
        int updatedRows = bookingRepository.atomicRejectPending(
                booking.getId(),
                BookingStatus.REJECTED,
                "booking.reject.reason.overtime",
                booking.getVersion()
        );

        if (updatedRows > 0) {
            syncBookingState(booking, BookingStatus.REJECTED);

            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    booking,
                    BookingStatus.REJECTED,
                    BookingAction.SYSTEM_REJECT.name(),
                    "SYSTEM",
                    "booking.reject.reason.overtime"
            ));
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, UserAccount userAccount) {
        log.info("{} | cancelBooking | booking id : {}", LogConstant.ACTION_START, bookingId);
        try {
            // lấy booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            // 1. Check quyền sở hữu
            if (!booking.getUser().getId().equals(userAccount.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            // 2. Lấy Slot bắt đầu sớm nhất
            TimeSlot firstSlot = booking.getBookingTimeSlots().stream()
                    .map(BookingTimeSlot::getTimeSlot)
                    .min(Comparator.comparing(TimeSlot::getStartTime))
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
            // 3. Gộp thành LocalDateTime để so sánh toàn diện (Ngày + Giờ)
            LocalDateTime startDateTime = booking.getBookingDate().atTime(firstSlot.getStartTime());

            bookingPolicyManager.validateCancelConditionPolicy(booking.getCreatedAt(), booking.getStatus(), startDateTime);

            int updatedRows = bookingRepository.atomicCancelByStudent(
                    booking.getId(),
                    BookingStatus.CANCELLED,
                    booking.getVersion()
            );

            if (updatedRows == 0) {
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }
            log.info("{} | cancelBooking | booking id : {}", LogConstant.ACTION_SUCCESS, bookingId);
            if (updatedRows > 0) {
                syncBookingState(booking, BookingStatus.CANCELLED);

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        booking,
                        BookingStatus.CANCELLED,
                        BookingAction.CANCEL_BOOKING.name(),
                        booking.getUser().getEmail(),
                        booking.getCancelledBy() // này do lí do cancel chứ ta ?
                ));
            }
            //TODO: bắn message với message "booking.status.cancelled"
        } catch (AppException e) {
            log.warn("{} | cancelBooking | Reason: {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        }
        catch (Exception e) {
            log.error("{} | cancelBooking | Unexpected Error System", LogConstant.SYS_ERROR, e);
            throw e;
        }
    }

    private void syncBookingState(Booking booking, BookingStatus newStatus) {
        booking.setStatus(newStatus);
        booking.setVersion(booking.getVersion() + 1);
    }

    private Map<TranslatableEntityType, Set<Long>> getBuildingTranslationIds(Building building) {
        if (building == null) return Collections.emptyMap();

        Map<TranslatableEntityType, Set<Long>> idsByType = new HashMap<>();
        // Ép kiểu ID từ Integer/Long sang Set<Long> để khớp với tham số của Service
        idsByType.put(TranslatableEntityType.BUILDING, Set.of(building.getId()));

        return idsByType;
    }
}
