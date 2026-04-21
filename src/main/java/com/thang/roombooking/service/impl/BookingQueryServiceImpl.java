package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.BookingMessageKeys;
import com.thang.roombooking.common.dto.request.AdminBookingSearchRequest;
import com.thang.roombooking.common.dto.request.BookingSearchRequest;
import com.thang.roombooking.common.dto.response.*;
import com.thang.roombooking.common.enums.BookingAction;
import com.thang.roombooking.common.enums.BookingSort;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.BookingHistoryRepository;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.BookingQueryService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingQueryServiceImpl implements BookingQueryService {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final TranslationService translationService;
    private final BookingMapper bookingMapper;

    // ── getBookingDetail ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(Long id, UserAccount currentUser) {
        log.info("getBookingDetail | bookingId={} | userId={}", id, currentUser.getId());

        // 1. Fetch booking – throw 404 if missing
        Booking booking = bookingRepository.findByIdWithTimeSlots(id)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

        // 2. Ownership check – only the owner (or ADMIN/STAFF) may view
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());
        boolean isPrivileged = currentUser.getRole() != null &&
                (currentUser.getRole().getName().equalsIgnoreCase("ADMIN") ||
                 currentUser.getRole().getName().equalsIgnoreCase("STAFF"));

        if (!isOwner && !isPrivileged) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        // 3. Build combined translation map (time-slots + building)
        Map<String, String> translations = buildTranslations(booking);

        // 4. Map main fields (timeSlots is ignored inside mapper – set below with translations)
        BookingDetailResponse response = bookingMapper.toBookingDetailResponse(booking, translations);

        // 5. Populate attendees from classroom capacity (Booking has no attendees column yet)
        if (booking.getClassroom() != null) {
            response.setAttendees(booking.getClassroom().getCapacity());
        }

        // 6. Build translated time slots – TimeSlotResponse is an immutable record,
        //    so we must build each instance fresh with the resolved slotName.
        response.setTimeSlots(buildTranslatedTimeSlots(booking, translations));

        // 7. Load audit history of a booking
        List<BookingHistorySummaryResponse> history = bookingHistoryRepository
                .findByBookingId(booking.getId())
                .stream()
                .map(this::mapToBookingHistorySummary)
                .toList();

        response.setBookingHistorySummaryResponses(history);

        return response;
    }

    // ── searchPublic ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResult<List<BookingDetailResponse>> searchPublic(BookingSearchRequest request, UserAccount currentUser) {
        log.info("searchPublic | userId={} | status={} | date={} | page={} | size={}",
                currentUser.getId(), request.getStatus(), request.getBookingDate(),
                request.getPage(), request.getSize());

        Specification<Booking> spec = buildSpecification(request, currentUser);
        Pageable pageable = buildPageable(request);

        Page<Booking> bookingsPage = bookingRepository.findAll(spec, pageable);

        // Build a single translation map for the whole page (batch fetch)
        Map<String, String> translations = buildPageTranslations(bookingsPage.getContent());

        List<BookingDetailResponse> items = bookingsPage.getContent().stream()
                .map(booking -> {
                    BookingDetailResponse response = bookingMapper.toBookingDetailResponse(booking, translations);
                    if (booking.getClassroom() != null) {
                        response.setAttendees(booking.getClassroom().getCapacity());
                    }
                    // Build translated time slots (same pattern as getBookingDetail)
                    response.setTimeSlots(buildTranslatedTimeSlots(booking, translations));

                    return response;
                })
                .toList();

        // Use the project's standard offset-pagination factory
        return ApiResult.success(
                items,
                bookingsPage.getNumber() + 1,           // convert 0-based back to 1-based for the client
                bookingsPage.getSize(),
                bookingsPage.getTotalElements()
        );
    }

    @Override
    public StudentDashboardResponse getStudentDashboard(Long userId) {
        LocalDate today = LocalDate.now();
        Pageable top5 = PageRequest.of(0, 5);

        // 1. Lấy dữ liệu Entity (đã được JOIN FETCH ở Repo)
        List<Booking> upcomingEntities = bookingRepository.findUpcomingBookings(userId, today, top5);
        List<Booking> recentActivities = bookingRepository.findRecentBookings(userId, today, top5);

        // 2. Trả về Dashboard DTO
        return StudentDashboardResponse.builder()
                .totalBookings(bookingRepository.countTotalByUser(userId))
                .upcomingBookings(bookingRepository.countUpcomingByUser(userId, today))
                .pendingBookings(bookingRepository.countPendingByUser(userId))
                .upcomingList(upcomingEntities.stream().map(this::mapToSummary).toList())
                .historyList(recentActivities.stream().map(this::mapToHistorySummary).toList())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public ApiResult<List<AdminBookingListResponse>> searchAdmin(AdminBookingSearchRequest request, UserAccount currentUser) {
        log.info("searchAdmin | adminUserId={} | bookingId={} | studentCode={} | classroomId={} | status={} | date={} | page={} | size={}",
                currentUser.getId(), request.getBookingId(), request.getStudentCode(), request.getClassroomId(),
                request.getStatus(), request.getBookingDate(), request.getPage(), request.getSize());

        Specification<Booking> spec = buildAdminSpecification(request);
        Pageable pageable = buildPageable(request);

        Page<Booking> bookingsPage = bookingRepository.findAll(spec, pageable);

        Map<String, String> translations = buildPageTranslations(bookingsPage.getContent());

        List<AdminBookingListResponse> items = bookingsPage.getContent().stream()
                .map(b -> toAdminBookingListResponse(b, translations))
                .toList();

        return ApiResult.success(
                items,
                bookingsPage.getNumber() + 1,
                bookingsPage.getSize(),
                bookingsPage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingDetailResponse getAdminBookingDetail(Long id, UserAccount currentUser) {
        log.info("getAdminBookingDetail | bookingId={} | adminUserId={}", id, currentUser.getId());

        Booking booking = bookingRepository.findByIdWithAdminDetail(id)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

        Map<String, String> translations = buildTranslations(booking);
        AdminBookingDetailResponse response = bookingMapper.toAdminBookingDetailResponse(booking, translations);
        response.setTimeSlots(buildTranslatedTimeSlots(booking, translations));
        return response;
    }

    /**
     * List row uses the first time slot by start time when a booking spans multiple slots; full slots are on the detail endpoint.
     */
    private AdminBookingListResponse toAdminBookingListResponse(Booking booking, Map<String, String> translations) {
        List<TimeSlotResponse> slots = buildTranslatedTimeSlots(booking, translations);

        return AdminBookingListResponse.builder()
                .id(booking.getId())
                .studentName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .room(bookingMapper.toAdminBookingRoomRequestedResponse(booking))
                .purpose(booking.getPurpose())
                .date(booking.getBookingDate())
                .timeSlots(slots)
                .status(booking.getStatus())
                .build();
    }

    private Specification<Booking> buildAdminSpecification(AdminBookingSearchRequest req) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("user", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("bookingTimeSlots", jakarta.persistence.criteria.JoinType.LEFT)
                        .fetch("timeSlot", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("classroom", jakarta.persistence.criteria.JoinType.LEFT)
                        .fetch("building", jakarta.persistence.criteria.JoinType.LEFT);
            }

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (hasText(req.getStudentCode())) {
                String pattern = "%" + req.getStudentCode().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("user").get("studentCode")), pattern));
            }

            if (req.getBookingId() != null) {
                predicates.add(cb.equal(root.get("id"), req.getBookingId()));
            }

            if (req.getClassroomId() != null) {
                predicates.add(cb.equal(root.get("classroom").get("id"), req.getClassroomId()));
            }

            if (req.getBookingDate() != null) {
                predicates.add(cb.equal(root.get("bookingDate"), req.getBookingDate()));
            }

            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }

            if (req.getTimeSlotId() != null) {
                var btsJoin = root.join("bookingTimeSlots", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.equal(btsJoin.get("timeSlot").get("id"), req.getTimeSlotId()));
                query.distinct(true);
            }

            if (req.getAttendees() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("attendees"), req.getAttendees()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Pageable buildPageable(AdminBookingSearchRequest req) {
        int page = Math.max(req.getPage() - 1, 0);
        Sort sort = buildSort(req.getSort());
        return PageRequest.of(page, req.getSize(), sort);
    }

    // Map đơn sắp tới
    private BookingSummaryResponse mapToSummary(Booking b) {
        return new BookingSummaryResponse(
                b.getId(),
                getTranslatedRoomName(b),
                getTranslatedBuilding(b),
                b.getBookingDate(),
                formatTimeRange(b),
                b.getStatus(),
                resolveNextDashboardAction(b.getStatus())
        );
    }

    private static String resolveNextDashboardAction(BookingStatus status) {
        if (status == BookingStatus.APPROVED) {
            return BookingAction.CHECK_IN.name();
        }
        if (status == BookingStatus.CHECKED_IN) {
            return BookingAction.CHECK_OUT.name();
        }
        return null;
    }

    private String getTranslatedBuilding(Booking b) {
        if (b == null || b.getClassroom().getBuilding() == null) {
            return null;
        }

        Long buildingId = b.getClassroom().getBuilding().getId();
        String translated = translationService.getTranslation(
                TranslatableEntityType.BUILDING,
                buildingId,
                "name");

        if (translated == null) {
            translated = translationService.getTranslation(
                    TranslatableEntityType.CLASSROOM,
                    buildingId,
                    "buildingName");
        }

        return translated != null ? translated : b.getClassroom().getRoomName();
    }

    private String getTranslatedRoomName(Booking b) {
        if (b == null || b.getClassroom() == null) {
            return null;
        }

        Long classroomId = b.getClassroom().getId();
        String translated = translationService.getTranslation(
                TranslatableEntityType.CLASSROOM,
                classroomId,
                "name");

        if (translated == null) {
            translated = translationService.getTranslation(
                    TranslatableEntityType.CLASSROOM,
                    classroomId,
                    "roomName");
        }

        return translated != null ? translated : b.getClassroom().getRoomName();
    }

    private BookingRecentSummaryResponse mapToHistorySummary(Booking b) {
        String message = resolveRecentHistoryMessage(b);

        return new BookingRecentSummaryResponse(
                b.getId(),
                getTranslatedRoomName(b),
                getTranslatedBuilding(b),
                resolveDashboardHistoryAction(b.getStatus()),
                b.getStatus(),
                b.getUpdatedAt(),
                message
        );
    }

    // Semantic action for UI (aligned with BookingAction / history)
    private static String resolveDashboardHistoryAction(BookingStatus status) {
        return switch (status) {
            case COMPLETED -> BookingAction.CHECK_OUT.name();
            case CHECKED_IN -> BookingAction.CHECK_IN.name();
            case PENDING -> BookingAction.CREATE_BOOKING.name();
            case APPROVED -> BookingAction.APPROVE_BOOKING.name();
            case CANCELLED -> BookingAction.CANCEL_BOOKING.name();
            case REJECTED -> BookingAction.REJECT_BOOKING.name();
        };
    }

    // Map lịch sử chi tiết cho đơn
    private BookingHistorySummaryResponse mapToBookingHistorySummary(BookingHistory bh) {
        return new BookingHistorySummaryResponse(
                bh.getBooking().getId(),
                getTranslatedRoomName(bh.getBooking()),
                bh.getAction(),
                bh.getStatusAfter().name(),
                bh.getCreatedAt(), // Mốc thời gian tạo history
//                hasText(bh.getNote()) ? (isI18nKey(bh.getNote()) ? I18nUtils.get(bh.getNote()) : bh.getNote()) : I18nUtils.get(BookingMessageKeys.HISTORY_NOTE_DEFAULT),
                resolveNote(bh.getNote()),
                bh.getPerformedBy()
        );
    }

    private String resolveNote(String note) {
        if (hasText(note)) {
            return isI18nKey(note) ? I18nUtils.get(note) : note;
        }
        return I18nUtils.get(BookingMessageKeys.HISTORY_NOTE_DEFAULT);
    }

    private static boolean isI18nKey(String value) {
        return value != null && value.startsWith("booking.");
    }

    private String resolveRecentHistoryMessage(Booking b) {
        return switch (b.getStatus()) {
            case REJECTED -> handleRejectedCase(b.getRejectionReason());
            //                      hasText(b.getRejectionReason())
            //                    ? (isI18nKey(b.getRejectionReason())
            //                    ? I18nUtils.get(b.getRejectionReason())
            //                    : b.getRejectionReason())
            //                    : I18nUtils.get(BookingMessageKeys.HISTORY_REJECTED_NO_REASON);
            case PENDING -> I18nUtils.get(BookingMessageKeys.HISTORY_PENDING);
            case CANCELLED -> I18nUtils.get(BookingMessageKeys.HISTORY_CANCELLED);
            case APPROVED -> I18nUtils.get(BookingMessageKeys.HISTORY_APPROVED);
            case CHECKED_IN -> I18nUtils.get(BookingMessageKeys.HISTORY_CHECKED_IN);
            case COMPLETED -> I18nUtils.get(BookingMessageKeys.HISTORY_COMPLETED);
        };
    }

    private String handleRejectedCase(String rejectionReason) {
        if (hasText(rejectionReason)) {
            if (isI18nKey(rejectionReason)) {
                return I18nUtils.get(rejectionReason);
            } else {
                return rejectionReason;
            }
        }
        return I18nUtils.get(BookingMessageKeys.HISTORY_REJECTED_NO_REASON);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Hàm format "07:00 - 09:00" từ danh sách TimeSlots
    private String formatTimeRange(Booking b) {
        if (b.getBookingTimeSlots() == null || b.getBookingTimeSlots().isEmpty()) return "";

        var sortedSlots = b.getBookingTimeSlots().stream()
                .map(BookingTimeSlot::getTimeSlot)
                .sorted(java.util.Comparator.comparing(TimeSlot::getStartTime))
                .toList();

        return sortedSlots.getFirst().getStartTime() + " - " +
                sortedSlots.getLast().getEndTime();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    // ── Time-slot helpers ────────────────────────────────────────────────────

    /**
     * Rebuilds the list of {@link TimeSlotResponse} records with translated {@code slotName}.
     * <p>
     * We cannot mutate {@link TimeSlotResponse} after construction (it is a Java record),
     * so we must build each instance fresh here in the service layer where we have direct
     * access to the {@code translations} map.
     * <p>
     * Key format: {@code "TIME_SLOT_{id}_slotName"} — must match
     * {@link com.thang.roombooking.service.TranslationService#getAllTimeSlotTranslations()}.
     */
    private List<TimeSlotResponse> buildTranslatedTimeSlots(
            Booking booking, Map<String, String> translations) {

        if (booking.getBookingTimeSlots() == null || booking.getBookingTimeSlots().isEmpty()) {
            return List.of();
        }

        return booking.getBookingTimeSlots().stream()
                .sorted(java.util.Comparator.comparing(
                        bts -> bts.getTimeSlot().getStartTime()))
                .map(bts -> {
                    var slot = bts.getTimeSlot();
                    String key = "TIME_SLOT_" + slot.getId() + "_name";
                    String slotName = translations.getOrDefault(key, slot.getSlotNameKey());
                    return TimeSlotResponse.builder()
                            .id(slot.getId())
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .slotName(slotName)
                            .build();
                })
                .toList();
    }

    /**
     * Builds a combined translation map for a single booking
     * (time-slot translations + building name translation).
     */
    private Map<String, String> buildTranslations(Booking booking) {
        Map<String, String> timeSlotTranslations = translationService.getAllTimeSlotTranslations();

        Map<String, String> buildingTranslations = Collections.emptyMap();
        if (booking.getClassroom() != null && booking.getClassroom().getBuilding() != null) {
            Building building = booking.getClassroom().getBuilding();
            buildingTranslations = translationService.getTranslations(
                    Map.of(TranslatableEntityType.BUILDING, Set.of(building.getId()))
            );
        }

        Map<String, String> combined = new HashMap<>(timeSlotTranslations);
        combined.putAll(buildingTranslations);
        return combined;
    }

    /**
     * Builds a combined translation map for a page of bookings (batch-efficient).
     */
    private Map<String, String> buildPageTranslations(List<Booking> bookings) {
        Map<String, String> timeSlotTranslations = translationService.getAllTimeSlotTranslations();

        // Collect all distinct building IDs
        java.util.Set<Long> buildingIds = new java.util.HashSet<>();
        for (Booking b : bookings) {
            if (b.getClassroom() != null && b.getClassroom().getBuilding() != null) {
                buildingIds.add(b.getClassroom().getBuilding().getId());
            }
        }

        Map<String, String> buildingTranslations = buildingIds.isEmpty()
                ? Collections.emptyMap()
                : translationService.getTranslations(
                        Map.of(TranslatableEntityType.BUILDING, buildingIds));

        Map<String, String> combined = new HashMap<>(timeSlotTranslations);
        combined.putAll(buildingTranslations);
        return combined;
    }

    /**
     * Builds a JPA {@link Specification} scoped to the current user's bookings
     * with optional filters from {@link BookingSearchRequest}.
     */
    private Specification<Booking> buildSpecification(BookingSearchRequest req, UserAccount currentUser) {
        return (root, query, cb) -> {
            // Eagerly fetch time slots and classroom
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingTimeSlots", jakarta.persistence.criteria.JoinType.LEFT)
                    .fetch("timeSlot", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("classroom", jakarta.persistence.criteria.JoinType.LEFT)
                    .fetch("building", jakarta.persistence.criteria.JoinType.LEFT);
            }

            // Always scope to current user
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("user").get("id"), currentUser.getId()));

            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }

            if (req.getBookingDate() != null) {
                predicates.add(cb.equal(root.get("bookingDate"), req.getBookingDate()));
            }

            if (req.getTimeSlotId() != null) {
                // JOIN bookingTimeSlots -> timeSlot
                var btsJoin = root.join("bookingTimeSlots", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.equal(btsJoin.get("timeSlot").get("id"), req.getTimeSlotId()));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Converts the request's 1-indexed page number to a Spring {@link Pageable}.
     */
    private Pageable buildPageable(BookingSearchRequest req) {
        int page = Math.max(req.getPage() - 1, 0); // convert 1-based → 0-based
        Sort sort = buildSort(req.getSort());
        return PageRequest.of(page, req.getSize(), sort);
    }

    private Sort buildSort(BookingSort bookingSort) {
        if (bookingSort == null) return Sort.by("createdAt").descending();
        return switch (bookingSort) {
            case BOOKING_DATE_ASC  -> Sort.by("bookingDate").ascending();
            case BOOKING_DATE_DESC -> Sort.by("bookingDate").descending();
            case STATUS_ASC        -> Sort.by("status").ascending();
            case STATUS_DESC       -> Sort.by("status").descending();
            default                -> Sort.by("createdAt").descending();
        };
    }
}
