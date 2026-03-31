package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.BookingSearchRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.BookingApprovalResponse;
import com.thang.roombooking.common.dto.response.BookingDetailResponse;
import com.thang.roombooking.common.dto.response.TimeSlotResponse;
import com.thang.roombooking.common.enums.BookingSort;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.BookingApprovalRepository;
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
    private final BookingApprovalRepository bookingApprovalRepository;
    private final TranslationService translationService;
    private final BookingMapper bookingMapper;

    // ── getBookingDetail ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(Long id, UserAccount currentUser) {
        log.info("getBookingDetail | bookingId={} | userId={}", id, currentUser.getId());

        // 1. Fetch booking – throw 404 if missing
        Booking booking = bookingRepository.findById(id)
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

        // 7. Load approval / audit history
        List<BookingApprovalResponse> approvalHistory = bookingApprovalRepository
                .findByBookingId(booking.getId())
                .stream()
                .map(bookingMapper::toBookingApprovalResponse)
                .toList();
        response.setApprovalHistory(approvalHistory);

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
                    // Approval history intentionally omitted in list view (performance)
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
                    String key = "TIME_SLOT_" + slot.getId() + "_slotName";
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
                if (query != null) query.distinct(true);
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
