package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.BookingApprovalRequest;
import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.request.CheckoutRequest;
import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.enums.*;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.service.*;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
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
            bookingPolicyManager.validateBookingTimeWorkingHours(request.bookingDate(), request.timeBooking());

            // 2. Policy Quota & Penalty
            bookingPolicyManager.validatePenalty(currentUser.getId()); //TODO: tạm thời luôn cho qua chưa tính tới
            bookingPolicyManager.validateNoOverlappingActiveBookings(currentUser.getId(), request.bookingDate(), request.timeSlotIds());
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
                    .purpose(cleanString(request.purpose()))
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
                    "booking.create.reason.pending",
                    LocaleContextHolder.getLocale().toString()
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
            // 1) Load booking WITH time slots in one query (source of truth for start time)
            Booking booking = bookingRepository.findByIdWithTimeSlots(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateCheckInStatus(booking.getStatus());

            // Check quyền sở hữu
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            // 2) Derive the booking's effective start time from its TimeSlots (source of truth).
            // The denormalized booking.startTime column is nullable (see V9/V11 migrations) and
            // cannot be relied upon. We must resolve the earliest start time from the related slots.
            ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");

            LocalTime resolvedStartTime = booking.getBookingTimeSlots().stream()
                    .map(BookingTimeSlot::getTimeSlot)
                    .map(TimeSlot::getStartTime)
                    .min(LocalTime::compareTo)
                    .orElseGet(() -> {
                        // Fallback: if slots are not loaded, use the denormalized column
                        if (booking.getStartTime() != null) {
                            log.warn("[checkIn] No BookingTimeSlots loaded for booking={}, falling back to denormalized startTime", booking.getId());
                            return booking.getStartTime();
                        }
                        throw new AppException(BookingErrorCode.BOOKING_NOT_FOUND);
                    });

            LocalDateTime startDateTime = booking.getBookingDate().atTime(resolvedStartTime);
            Instant startInstant = startDateTime.atZone(vnZone).toInstant();
            bookingPolicyManager.validateCheckInTimePolicy(startInstant);

            // 3. ATOMIC UPDATE: Chặn đứng mọi nỗ lực duplicate request
            Instant checkinTime = request.checkInTime() != null ? request.checkInTime() : Instant.now();
            int updatedRows = bookingRepository.atomicCheckIn(booking.getId(), checkinTime, booking.getVersion());

            if (updatedRows == 0) {
                // Nếu đã CHECKED_IN rồi thì status không còn là APPROVED -> updatedRows = 0
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_CHECKED_IN);
            }

            if (updatedRows > 0) {
                // Update entity in memory to ensure events/logs have fresh data
                booking.setStatus(BookingStatus.CHECKED_IN);
                booking.setCheckinTime(checkinTime);
                booking.setVersion(booking.getVersion() + 1);

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        booking,
                        BookingStatus.CHECKED_IN,
                        BookingAction.CHECK_IN.name(),
                        currentUser.getEmail(),
                        "booking.checkin.reason.success",
                        LocaleContextHolder.getLocale().toString()
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
    public void checkout(CheckoutRequest request, UserAccount currentUser) {
        log.info("{} | Checkout Booking | User: {} | Data: {}",
                LogConstant.ACTION_START, currentUser.getId(), request);
        try {
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            if (!booking.getUser().getId().equals(currentUser.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            if (booking.getStatus() != BookingStatus.CHECKED_IN) {
                throw new AppException(BookingErrorCode.BOOKING_NOT_CHECKED_IN);
            }

            Instant checkoutTime = request.checkoutTime() != null ? request.checkoutTime() : Instant.now();
            int updatedRows = bookingRepository.atomicCheckoutToCompleted(booking.getId(), checkoutTime, booking.getVersion());
            if (updatedRows == 0) {
                // already checked out or version mismatch
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }

            if (updatedRows > 0) {
                // Update entity in memory
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setCheckoutTime(checkoutTime);
                booking.setVersion(booking.getVersion() + 1);

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        booking,
                        BookingStatus.COMPLETED,
                        BookingAction.CHECK_OUT.name(),
                        currentUser.getEmail(),
                        "booking.checkout.reason.success",
                        LocaleContextHolder.getLocale().toString()
                ));
            }

            log.info("{}: Booking checkout with ID: {} for User: {}", LogConstant.ACTION_SUCCESS, booking.getId(), currentUser.getId());
        } catch (AppException e) {
            log.warn("{}: Failed to checkout booking for User: {}. Reason: {}", LogConstant.BIZ_ERROR,
                    currentUser.getId(), e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Unexpected error during booking checkout for User: {}", LogConstant.SYS_ERROR,
                    currentUser.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveBooking(BookingApprovalRequest request, UserAccount currentUser) {
        log.info("{}: Booking approve with ID: {} by STAFF: {}",LogConstant.ACTION_START, request.bookingId(), currentUser.getId());
        try {
            // lấy booking
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateApproveStatus(booking.getStatus());

            if (request.action() != ApprovalAction.APPROVE) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, request.action().name());
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

            Booking approvedBooking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            log.info("{} | Booking ID: {} approved successfully", LogConstant.ACTION_SUCCESS, request.bookingId());
            bookingApprovalCommandService.saveApprovalBooking(approvedBooking, currentUser);
            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    approvedBooking,
                    BookingStatus.APPROVED,
                    BookingAction.APPROVE_BOOKING.name(),
                    currentUser.getEmail(),
                    "booking.approve.reason.staff",
                    LocaleContextHolder.getLocale().toString()
            ));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBooking(BookingApprovalRequest request, UserAccount currentUser) {
        log.info("{}: Booking reject with ID: {} by STAFF: {}", LogConstant.ACTION_START, request.bookingId(), currentUser.getId());
        try {
            Booking booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateRejectStatus(booking.getStatus());

            if (request.action() != ApprovalAction.REJECT) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, request.action().name());
            }

            String cleanReason = cleanString(request.reason());
            if (cleanReason == null || cleanReason.isBlank()) {
                throw new AppException(BookingErrorCode.BOOKING_REJECTION_REASON_REQUIRED);
            }

            // Perform Atomic Update
            int updatedRows = bookingRepository.atomicRejectPending(
                    booking.getId(),
                    BookingStatus.REJECTED,
                    cleanReason,
                    booking.getVersion()
            );

            if (updatedRows == 0) {
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }

            // Update entity in memory for events
            booking.setStatus(BookingStatus.REJECTED);
            booking.setRejectionReason(cleanReason);
            booking.setVersion(booking.getVersion() + 1);

            log.info("{} | Booking ID: {} rejected successfully", LogConstant.ACTION_SUCCESS, request.bookingId());
            bookingApprovalCommandService.saveApprovalBooking(booking, currentUser);
            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    booking,
                    BookingStatus.REJECTED,
                    BookingAction.REJECT_BOOKING.name(),
                    currentUser.getEmail(),
                    "booking.reject.reason.staff",
                    LocaleContextHolder.getLocale().toString()
            ));
        } catch (AppException e) {
            log.warn("{}: Failed to reject | Reason: {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("{} | Conflict Error |", LogConstant.SYS_ERROR, e.getCause());
            throw e;
        } catch (Exception e) {
            log.error("{} | Unexpected System Error during rejection |", LogConstant.SYS_ERROR, e);
            throw e;
        }
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
                    "booking.cancel.reason.no_show",
                    Locale.getDefault().toString()
            ));
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
            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    booking,
                    BookingStatus.REJECTED,
                    BookingAction.SYSTEM_REJECT.name(),
                    "SYSTEM",
                    "booking.reject.reason.overtime",
                    Locale.getDefault().toString()
            ));
        }
    }

    @Override
    @Transactional
    public void autoCheckoutExpiredBooking(Booking booking) {
        // Best-effort: checkoutTime = now (server time)
        try {
            int updatedRows = bookingRepository.atomicCheckoutToCompleted(booking.getId(), Instant.now(), booking.getVersion());
            if (updatedRows > 0) {
                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        booking,
                        BookingStatus.COMPLETED,
                        BookingAction.CHECK_OUT.name(),
                        "SYSTEM",
                        "booking.checkout.reason.auto",
                        Locale.getDefault().toString()
                ));
            }
        } catch (Exception e) {
            log.error("{} | autoCheckout failed for booking {}: {}", LogConstant.SYS_ERROR, booking.getId(), e.getMessage());
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
                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        booking,
                        BookingStatus.CANCELLED,
                        BookingAction.CANCEL_BOOKING.name(),
                        booking.getUser().getEmail(),
                        booking.getCancelledBy(),
                        LocaleContextHolder.getLocale().toString()
                ));
            }
        } catch (AppException e) {
            log.warn("{} | cancelBooking | Reason: {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        }
        catch (Exception e) {
            log.error("{} | cancelBooking | Unexpected Error System", LogConstant.SYS_ERROR, e);
            throw e;
        }
    }


    private String cleanString(String data) {
        return data != null ? data.trim() : null;
    }

    private Map<TranslatableEntityType, Set<Long>> getBuildingTranslationIds(Building building) {
        if (building == null) return Collections.emptyMap();

        Map<TranslatableEntityType, Set<Long>> idsByType = new HashMap<>();
        // Ép kiểu ID từ Integer/Long sang Set<Long> để khớp với tham số của Service
        idsByType.put(TranslatableEntityType.BUILDING, Set.of(building.getId()));

        return idsByType;
    }
}
