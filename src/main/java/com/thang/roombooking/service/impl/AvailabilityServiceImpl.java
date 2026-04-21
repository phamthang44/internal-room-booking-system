package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.ClassroomAvailabilityResponse;
import com.thang.roombooking.common.dto.response.DateAvailability;
import com.thang.roombooking.common.dto.response.SlotStatus;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.SlotBookingStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.utils.TranslationKeyBuilder;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.TimeSlot;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.TimeSlotRepository;
import com.thang.roombooking.service.AvailabilityService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TranslationService translationService;

    @Transactional(readOnly = true)
    @Override
    public ClassroomAvailabilityResponse getClassroomAvailability(Long classroomId, LocalDate startDate, LocalDate endDate) {
        List<TimeSlot> allSlots = getSortedTimeSlots();
        Map<String, String> translations = getTimeSlotTranslations(allSlots);

        Map<LocalDate, List<SlotStatus>> availabilityMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            availabilityMap.put(date, createDailySlots(allSlots, translations));
        }

        // include REJECTED to show student why their booking was denied.
        List<BookingStatus> relevantStatuses = Arrays.asList(
                BookingStatus.APPROVED, 
                BookingStatus.PENDING, 
                BookingStatus.CHECKED_IN,
                BookingStatus.COMPLETED,
                BookingStatus.REJECTED
        );
        List<Booking> overlappingBookings = bookingRepository.findBookingsByClassroomAndDateRange(
                classroomId, startDate, endDate, relevantStatuses
        );

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vnZone);
        LocalTime nowTime = LocalTime.now(vnZone);

        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isViewerAdmin();

        for (Booking booking : overlappingBookings) {
            LocalDate bDate = booking.getBookingDate();
            List<SlotStatus> daySlots = availabilityMap.get(bDate);
            if (daySlots != null) {
                applyBookingToDaySlots(booking, daySlots, bDate, today, nowTime, currentUserId, isAdmin);
            }
        }

        return assembleFinalOutput(startDate, availabilityMap);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<Long, DateAvailability> getBulkClassroomsAvailabilityForDate(List<Long> classroomIds, LocalDate date) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<TimeSlot> allSlots = getSortedTimeSlots();
        Map<String, String> translations = getTimeSlotTranslations(allSlots);

        Map<Long, DateAvailability> bulkMap = new HashMap<>();
        for (Long cId : classroomIds) {
            bulkMap.put(cId, new DateAvailability(date, createDailySlots(allSlots, translations)));
        }

        // include REJECTED to show student why their booking was denied.
        List<BookingStatus> relevantStatuses = Arrays.asList(
                BookingStatus.APPROVED, 
                BookingStatus.PENDING, 
                BookingStatus.CHECKED_IN,
                BookingStatus.COMPLETED,
                BookingStatus.REJECTED
        );
        List<Booking> overlappingBookings = bookingRepository.findBookingsByClassroomIdsAndDate(
                classroomIds, date, relevantStatuses
        );

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vnZone);
        LocalTime nowTime = LocalTime.now(vnZone);

        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isViewerAdmin();

        for (Booking booking : overlappingBookings) {
            Long cId = booking.getClassroom().getId();
            DateAvailability classroomDateGrid = bulkMap.get(cId);
            if (classroomDateGrid != null) {
                applyBookingToDaySlots(booking, classroomDateGrid.slots(), date, today, nowTime, currentUserId, isAdmin);
            }
        }

        return bulkMap;
    }

    private List<TimeSlot> getSortedTimeSlots() {
        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        allSlots.sort(Comparator.comparing(TimeSlot::getStartTime));
        return allSlots;
    }

    private Map<String, String> getTimeSlotTranslations(List<TimeSlot> allSlots) {
        Set<Long> slotIds = allSlots.stream()
                .map(s -> Long.valueOf(s.getId()))
                .collect(Collectors.toSet());
        Map<TranslatableEntityType, Set<Long>> typeMap = new EnumMap<>(TranslatableEntityType.class);
        typeMap.put(TranslatableEntityType.TIME_SLOT, slotIds);
        return translationService.getTranslations(typeMap);
    }

    private List<SlotStatus> createDailySlots(List<TimeSlot> allSlots, Map<String, String> translations) {
        List<SlotStatus> dailySlots = new ArrayList<>();
        for (TimeSlot slot : allSlots) {
            String translationKey = TranslationKeyBuilder.build(TranslatableEntityType.TIME_SLOT, Long.valueOf(slot.getId()), "name");
            String translatedSlotName = translations.getOrDefault(translationKey, slot.getSlotNameKey());
            
            dailySlots.add(new SlotStatus(
                    Long.valueOf(slot.getId()),
                    translatedSlotName,
                    slot.getStartTime() != null ? slot.getStartTime().plusHours(7) : null,
                    slot.getEndTime() != null ? slot.getEndTime().plusHours(7) : null,
                    SlotBookingStatus.AVAILABLE,
                    true,
                    null,
                    null,
                    false
            ));

        }
        return dailySlots;
    }

    private void applyBookingToDaySlots(
            Booking booking,
            List<SlotStatus> daySlots,
            LocalDate bookingDate,
            LocalDate today,
            LocalTime nowTime,
            Long currentUserId,
            boolean isAdmin
    ) {
        booking.getBookingTimeSlots().forEach(bts -> {
            Long bSlotId = Long.valueOf(bts.getTimeSlot().getId());
            LocalTime slotStart = bts.getTimeSlot().getStartTime() != null ? bts.getTimeSlot().getStartTime().plusHours(7) : null;
            LocalTime slotEnd = bts.getTimeSlot().getEndTime() != null ? bts.getTimeSlot().getEndTime().plusHours(7) : null;

            if (isInactiveCheckedIn(booking, bookingDate, today, nowTime, slotStart, slotEnd)) {
                return;
            }

            updateSlotStatus(daySlots, bSlotId, booking, currentUserId, isAdmin);
        });
    }

    private boolean isInactiveCheckedIn(
            Booking booking,
            LocalDate bookingDate,
            LocalDate today,
            LocalTime nowTime,
            LocalTime slotStart,
            LocalTime slotEnd
    ) {
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            return false;
        }
        // Do not block if already checked out or not for today
        if (booking.getCheckoutTime() != null || !Objects.equals(bookingDate, today)) {
            return true;
        }
        // Only show IN_USE for active time window
        boolean inUseNow = (nowTime.equals(slotStart) || nowTime.isAfter(slotStart))
                && nowTime.isBefore(slotEnd);
        return !inUseNow;
    }

    private void updateSlotStatus(
            List<SlotStatus> daySlots,
            Long bSlotId,
            Booking booking,
            Long currentUserId,
            boolean isAdmin
    ) {
        for (int i = 0; i < daySlots.size(); i++) {
            SlotStatus current = daySlots.get(i);
            if (current.slotId().equals(bSlotId)) {
                SlotStatusUpdate update = getStatusUpdate(booking, currentUserId, isAdmin);
                if (update == null) {
                    return;
                }

                // Priority logic: if the slot is already blocked (isAvailable=false),
                // an AVAILABLE status (like REJECTED) should NOT overwrite it.
                if (!current.isAvailable() && update.isAvailable()) {
                    return;
                }

                daySlots.set(i, new SlotStatus(
                        current.slotId(),
                        current.slotName(),
                        current.startTime(),
                        current.endTime(),
                        update.status(),
                        update.isAvailable(),
                        booking.getId(),
                        update.rejectionReason(),
                        update.isMine()
                ));
                break;
            }
        }
    }

    private record SlotStatusUpdate(
            SlotBookingStatus status,
            boolean isAvailable,
            String rejectionReason,
            boolean isMine
    ) {}

    private SlotStatusUpdate getStatusUpdate(Booking booking, Long currentUserId, boolean isAdmin) {
        boolean isOwner = currentUserId != null && currentUserId.equals(booking.getUser().getId());

        return switch (booking.getStatus()) {
            case CHECKED_IN -> new SlotStatusUpdate(SlotBookingStatus.IN_USE, false, null, isOwner);
            case PENDING -> (isAdmin || isOwner)
                    ? new SlotStatusUpdate(SlotBookingStatus.PENDING, false, null, isOwner)
                    : new SlotStatusUpdate(SlotBookingStatus.OCCUPIED, false, null, false);
            case REJECTED -> isOwner
                    ? new SlotStatusUpdate(SlotBookingStatus.REJECTED, true, booking.getRejectionReason(), true)
                    : null; // Don't show other people's rejections in the grid
            case APPROVED, COMPLETED -> new SlotStatusUpdate(SlotBookingStatus.OCCUPIED, false, null, isOwner);
            default -> null;
        };
    }



    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUserDetails sud) {
            return sud.getUser().getId();
        }
        return null;
    }

    private boolean isViewerAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_STAFF") ||
                        a.getAuthority().equals("ROLE_MANAGER"));
    }

    private ClassroomAvailabilityResponse assembleFinalOutput(LocalDate startDate, Map<LocalDate, List<SlotStatus>> availabilityMap) {
        List<DateAvailability> availabilities = new ArrayList<>();
        boolean anyAvailableGlobally = false;
        
        for (Map.Entry<LocalDate, List<SlotStatus>> entry : availabilityMap.entrySet()) {
            availabilities.add(new DateAvailability(entry.getKey(), entry.getValue()));
            for (SlotStatus s : entry.getValue()) {
                if (s.isAvailable()) {
                    anyAvailableGlobally = true;
                    break;
                }
            }
        }

        return new ClassroomAvailabilityResponse(startDate, !anyAvailableGlobally, availabilities);
    }
}
