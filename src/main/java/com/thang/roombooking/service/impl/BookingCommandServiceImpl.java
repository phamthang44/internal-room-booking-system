package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.*;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.enums.*;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.repository.BookingRepository;
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

import static com.thang.roombooking.common.constant.SystemConstant.SYSTEM_ACTOR;
import static com.thang.roombooking.common.constant.SystemConstant.SYSTEM_REGION_TIMEZONE;

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
            bookingPolicyManager.validatePenalty(currentUser.getId());
            bookingPolicyManager.validatePendingQuota(currentUser.getId());
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

            // Convert to UTC before saving to DB (assuming VN is +7)
            ZoneId vnZone = ZoneId.of(SYSTEM_REGION_TIMEZONE);
            LocalTime utcStartTime = request.bookingDate().atTime(bookingStartTime).atZone(vnZone)
                    .withZoneSameInstant(ZoneId.of(SYSTEM_REGION_TIMEZONE)).toLocalTime();
            LocalTime utcEndTime = request.bookingDate().atTime(bookingEndTime).atZone(vnZone)
                    .withZoneSameInstant(ZoneId.of(SYSTEM_REGION_TIMEZONE)).toLocalTime();

            // 4. Build Entity với nguyên tắc XOR
            Booking booking = Booking.builder()
                    .user(currentUser)
                    .classroom(classroomRepository.getReferenceById(request.classroomId()))
                    .bookingDate(request.bookingDate())
                    .startTime(utcStartTime)
                    .endTime(utcEndTime)
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
    public void checkIn(Long id, CheckInRequest request, UserAccount currentUser) {
        log.info("{} | Check in Booking | ID: {} | User: {} | Data: {}",
                LogConstant.ACTION_START, id, currentUser.getId(), request);
        try {
            // 1) Load booking WITH time slots in one query (source of truth for start time)
            Booking booking = bookingRepository.findByIdWithTimeSlots(id)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateCheckInStatus(booking.getStatus());

            // Check quyền sở hữu
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            // 1.5) Ban check: Even if booking was approved, a banned user cannot check in.
            bookingPolicyManager.validatePenalty(currentUser.getId());

            // 2) Derive the booking's effective start time
            Instant startInstant = getBookingStartInstant(booking);
            bookingPolicyManager.validateCheckInTimePolicy(startInstant);

            // 3. ATOMIC UPDATE: Chặn đứng mọi nỗ lực duplicate request
            Instant checkinTime = request.checkInTime() != null ? request.checkInTime() : Instant.now();
            int updatedRows = bookingRepository.atomicCheckIn(booking.getId(), checkinTime, booking.getVersion());

            if (updatedRows == 0) {
                // Determine actual reason for failure
                Booking currentBooking = bookingRepository.findById(booking.getId())
                        .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

                if (currentBooking.getStatus() == BookingStatus.CHECKED_IN || currentBooking.getStatus() == BookingStatus.COMPLETED) {
                    throw new AppException(BookingErrorCode.BOOKING_ALREADY_CHECKED_IN);
                } else if (currentBooking.getStatus() != BookingStatus.APPROVED) {
                    throw new AppException(BookingErrorCode.BOOKING_NOT_APPROVED);
                }
                throw new AppException(BookingErrorCode.BOOKING_STATUS_ALREADY_CANCELLED);
            }

            if (updatedRows > 0) {
                Booking updated = bookingRepository.findById(id)
                        .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        updated,
                        BookingStatus.CHECKED_IN,
                        BookingAction.CHECK_IN.name(),
                        currentUser.getEmail(),
                        "booking.checkin.reason.success",
                        LocaleContextHolder.getLocale().toString()
                ));
            }

            log.info("{}: Booking checkin with ID: {} for User: {}",LogConstant.ACTION_SUCCESS, id, currentUser.getId());
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
    public void checkout(Long id, CheckoutRequest request, UserAccount currentUser) {
        log.info("{} | Checkout Booking | ID: {} | User: {} | Data: {}",
                LogConstant.ACTION_START, id, currentUser.getId(), request);
        try {
            Booking booking = bookingRepository.findByIdWithTimeSlots(id)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            if (!booking.getUser().getId().equals(currentUser.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            if (booking.getStatus() != BookingStatus.CHECKED_IN) {
                throw new AppException(BookingErrorCode.BOOKING_NOT_CHECKED_IN);
            }

            // 2. Policy: Chặn trả phòng quá sớm
            Instant startInstant = getBookingStartInstant(booking);
            bookingPolicyManager.validateCheckOutTimePolicy(startInstant);

            Instant checkoutTime = request.checkoutTime() != null ? request.checkoutTime() : Instant.now();
            int updatedRows = bookingRepository.atomicCheckoutToCompleted(booking.getId(), checkoutTime, booking.getVersion());
            if (updatedRows == 0) {
                // Determine actual reason for failure
                Booking currentBooking = bookingRepository.findById(booking.getId())
                        .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

                if (currentBooking.getCheckoutTime() != null || currentBooking.getStatus() == BookingStatus.COMPLETED) {
                    throw new AppException(BookingErrorCode.BOOKING_ALREADY_CHECKED_OUT);
                }
                throw new AppException(BookingErrorCode.BOOKING_NOT_CHECKED_IN);
            }

            if (updatedRows > 0) {
                Booking updated = bookingRepository.findById(id)
                        .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        updated,
                        BookingStatus.COMPLETED,
                        BookingAction.CHECK_OUT.name(),
                        currentUser.getEmail(),
                        "booking.checkout.reason.success",
                        LocaleContextHolder.getLocale().toString()
                ));
            }

            log.info("{}: Booking checkout with ID: {} for User: {}", LogConstant.ACTION_SUCCESS, id, currentUser.getId());
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
    public Long approveBooking(Long id, BookingApprovalRequest request, UserAccount currentUser) {
        log.info("{}: Booking approve with ID: {} by STAFF: {}",LogConstant.ACTION_START, id, currentUser.getId());
        try {
            // lấy booking
            Booking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            bookingPolicyManager.validateApproveStatus(booking.getStatus());

            if (request.action() != ApprovalAction.APPROVE) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, request.action().name());
            }

            // 3. Thực hiện Atomic Update
            int updatedRows = bookingRepository.atomicApprove(
                    id,
                    BookingStatus.APPROVED,
                    booking.getVersion()
            );
            if (updatedRows == 0) {
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }

            Booking approvedBooking = bookingRepository.findById(id)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            log.info("{} | Booking ID: {} approved successfully", LogConstant.ACTION_SUCCESS, id);
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectBooking(Long id, BookingApprovalRequest request, UserAccount currentUser) {
        log.info("{}: Booking reject with ID: {} by STAFF: {}", LogConstant.ACTION_START, id, currentUser.getId());
        try {
            Booking booking = bookingRepository.findById(id)
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
                    id,
                    BookingStatus.REJECTED,
                    cleanReason,
                    booking.getVersion()
            );

            if (updatedRows == 0) {
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }
            Booking updated = bookingRepository.findById(id)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
            // Update entity in memory for events
            log.info("{} | Booking ID: {} rejected successfully", LogConstant.ACTION_SUCCESS, id);
            bookingApprovalCommandService.saveApprovalBooking(updated, currentUser);
            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    updated,
                    BookingStatus.REJECTED,
                    BookingAction.REJECT_BOOKING.name(),
                    currentUser.getEmail(),
                    cleanReason.isBlank() ? "booking.reject.reason.staff" : cleanReason,
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
                SYSTEM_ACTOR,
                booking.getVersion()
        );

        if (updatedRows > 0) {
            Booking updated = bookingRepository.findById(booking.getId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            // 3. Delegation to Policy Layer for Violation & Penalty logic
            bookingPolicyManager.handleNoShowViolation(updated);

            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    updated,
                    BookingStatus.CANCELLED,
                    BookingAction.CANCEL_BOOKING.name(),
                    SYSTEM_ACTOR,
                    "booking.cancel.reason.no_show",
                    Locale.getDefault().toString()
            ));
        }
    }

    @Transactional
    @Override
    public void autoRejectOverduePendingBooking(Booking booking) {
        // Atomic Update: Chỉ từ chối nếu status vẫn đang là PENDING
        int updatedRows = bookingRepository.atomicRejectPendingBySystem(
                booking.getId(),
                BookingStatus.REJECTED,
                "booking.reject.reason.overtime",
                SYSTEM_ACTOR,
                booking.getVersion()
        );

        if (updatedRows > 0) {
            Booking updated = bookingRepository.findById(booking.getId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));


            eventPublisher.publishEvent(new BookingStatusChangedEvent(
                    updated,
                    BookingStatus.REJECTED,
                    BookingAction.SYSTEM_REJECT.name(),
                    SYSTEM_ACTOR,
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
            int updatedRows = bookingRepository.atomicAutoCheckoutToCompleted(
                    booking.getId(),
                    Instant.now(),
                    SYSTEM_ACTOR,
                    booking.getVersion()
            );
            if (updatedRows > 0) {
                Booking updated = bookingRepository.findById(booking.getId())
                        .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        updated,
                        BookingStatus.COMPLETED,
                        BookingAction.CHECK_OUT.name(),
                        SYSTEM_ACTOR,
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
    public void cancelBooking(Long id, CancelBookingRequest req, UserAccount userAccount) {
        log.info("{} | cancelBooking | booking id : {}", LogConstant.ACTION_START, id);
        try {
            // lấy booking
            String cleanCancelReason = cleanString(req.cancelReason());

            bookingValidatorService.validatePurpose(cleanCancelReason);

            Booking booking = bookingRepository.findByIdWithTimeSlots(id)
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

            // 1. Check quyền sở hữu
            if (!booking.getUser().getId().equals(userAccount.getId())) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
            }

            // 2. Policy
            Instant startInstant = getBookingStartInstant(booking);

            bookingPolicyManager.validateCancelConditionPolicy(booking.getCreatedAt(), booking.getStatus(), startInstant);

            int updatedRows = bookingRepository.atomicCancelByStudent(
                    booking.getId(),
                    BookingStatus.CANCELLED,
                    userAccount.getUsername(),
                    userAccount.getUsername(),
                    booking.getVersion()
            );

            if (updatedRows == 0) {
                throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
            }
            log.info("{} | cancelBooking | booking id : {}", LogConstant.ACTION_SUCCESS, id);
            if (updatedRows > 0) {
                Booking updated = bookingRepository.findById(id)
                        .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

                eventPublisher.publishEvent(new BookingStatusChangedEvent(
                        updated,
                        BookingStatus.CANCELLED,
                        BookingAction.CANCEL_BOOKING.name(),
                        updated.getUser().getEmail(),
                        cleanCancelReason,
                        LocaleContextHolder.getLocale().toString()
                ));

                // 3. Spam Prevention
                bookingPolicyManager.checkCancellationSpam(updated);
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


    private Instant getBookingStartInstant(Booking booking) {
        LocalTime resolvedStartTime = booking.getBookingTimeSlots().stream()
                .map(BookingTimeSlot::getTimeSlot)
                .map(TimeSlot::getStartTime)
                .min(LocalTime::compareTo)
                .orElseGet(() -> {
                    if (booking.getStartTime() != null) {
                        return booking.getStartTime();
                    }
                    throw new AppException(BookingErrorCode.BOOKING_NOT_FOUND);
                });

        // Since we are standardizing the database to UTC, we no longer need complex Vietnam-to-UTC conversion here.
        // We assume resolvedStartTime is already in UTC if found in the DB.
        return booking.getBookingDate().atTime(resolvedStartTime).atZone(ZoneId.of(SYSTEM_REGION_TIMEZONE)).toInstant();
    }

    private String cleanString(String data) {
        return data != null ? data.trim() : null;
    }

    private Map<TranslatableEntityType, Set<Long>> getBuildingTranslationIds(Building building) {
        if (building == null) return Collections.emptyMap();

        Map<TranslatableEntityType, Set<Long>> idsByType = new EnumMap<>(TranslatableEntityType.class);
        // Ép kiểu ID từ Integer/Long sang Set<Long> để khớp với tham số của Service
        idsByType.put(TranslatableEntityType.BUILDING, Set.of(building.getId()));

        return idsByType;
    }
}
