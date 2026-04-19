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
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.TimeSlotRepository;
import com.thang.roombooking.service.AvailabilityService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // NOTE: include CHECKED_IN and COMPLETED to align with database exclusion constraint.
        List<BookingStatus> blockingStatuses = Arrays.asList(
                BookingStatus.APPROVED, 
                BookingStatus.PENDING, 
                BookingStatus.CHECKED_IN,
                BookingStatus.COMPLETED
        );
        List<Booking> overlappingBookings = bookingRepository.findBookingsByClassroomAndDateRange(
                classroomId, startDate, endDate, blockingStatuses
        );

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vnZone);
        LocalTime nowTime = LocalTime.now(vnZone);

        for (Booking booking : overlappingBookings) {
            LocalDate bDate = booking.getBookingDate();
            List<SlotStatus> daySlots = availabilityMap.get(bDate);
            if (daySlots != null) {
                applyBookingToDaySlots(booking, daySlots, bDate, today, nowTime);
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

        // NOTE: include CHECKED_IN and COMPLETED to align with database exclusion constraint.
        List<BookingStatus> blockingStatuses = Arrays.asList(
                BookingStatus.APPROVED, 
                BookingStatus.PENDING, 
                BookingStatus.CHECKED_IN,
                BookingStatus.COMPLETED
        );
        List<Booking> overlappingBookings = bookingRepository.findBookingsByClassroomIdsAndDate(
                classroomIds, date, blockingStatuses
        );

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vnZone);
        LocalTime nowTime = LocalTime.now(vnZone);

        for (Booking booking : overlappingBookings) {
            Long cId = booking.getClassroom().getId();
            DateAvailability classroomDateGrid = bulkMap.get(cId);
            if (classroomDateGrid != null) {
                applyBookingToDaySlots(booking, classroomDateGrid.slots(), date, today, nowTime);
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
                    slot.getStartTime(),
                    slot.getEndTime(),
                    SlotBookingStatus.AVAILABLE,
                    true,
                    null
            ));
        }
        return dailySlots;
    }

    private void applyBookingToDaySlots(
            Booking booking,
            List<SlotStatus> daySlots,
            LocalDate bookingDate,
            LocalDate today,
            LocalTime nowTime
    ) {
        booking.getBookingTimeSlots().forEach(bts -> {
            Long bSlotId = Long.valueOf(bts.getTimeSlot().getId());
            LocalTime slotStart = bts.getTimeSlot().getStartTime();
            LocalTime slotEnd = bts.getTimeSlot().getEndTime();

            if (booking.getStatus() == BookingStatus.CHECKED_IN) {
                // Do not block if already checked out
                if (booking.getCheckoutTime() != null) return;
                // Only show IN_USE for today's active time window
                if (!Objects.equals(bookingDate, today)) return;
                boolean inUseNow =
                        (nowTime.equals(slotStart) || nowTime.isAfter(slotStart))
                                && nowTime.isBefore(slotEnd);
                if (!inUseNow) return;
            }

            for (int i = 0; i < daySlots.size(); i++) {
                SlotStatus current = daySlots.get(i);
                if (current.slotId().equals(bSlotId)) {
                    SlotBookingStatus statusLabel =
                            booking.getStatus() == BookingStatus.CHECKED_IN
                                    ? SlotBookingStatus.IN_USE
                                    : SlotBookingStatus.valueOf(booking.getStatus().name());
                    daySlots.set(i, new SlotStatus(
                            current.slotId(),
                            current.slotName(),
                            current.startTime(),
                            current.endTime(),
                            statusLabel,
                            false,
                            booking.getId()
                    ));
                    break;
                }
            }
        });
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
