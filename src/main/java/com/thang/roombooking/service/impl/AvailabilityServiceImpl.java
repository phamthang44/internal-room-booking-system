package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.ClassroomAvailabilityResponse;
import com.thang.roombooking.common.dto.response.DateAvailability;
import com.thang.roombooking.common.dto.response.SlotStatus;
import com.thang.roombooking.common.enums.BookingStatus;
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
        
        // 1. Fetch available timeslots
        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        allSlots.sort(Comparator.comparing(TimeSlot::getStartTime));

        // 2. Fetch translations for timeslots
        Set<Long> slotIds = allSlots.stream()
                .map(s -> Long.valueOf(s.getId()))
                .collect(Collectors.toSet());
        Map<TranslatableEntityType, Set<Long>> typeMap = new EnumMap<>(TranslatableEntityType.class);
        typeMap.put(TranslatableEntityType.TIME_SLOT, slotIds);
        Map<String, String> translations = translationService.getTranslations(typeMap);

        // 3. Initialize weekly availability skeleton
        Map<LocalDate, List<SlotStatus>> availabilityMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<SlotStatus> dailySlots = new ArrayList<>();
            for (TimeSlot slot : allSlots) {
                String translationKey = TranslationKeyBuilder.build(TranslatableEntityType.TIME_SLOT, Long.valueOf(slot.getId()), "name");
                // The DB logic from original code uses "TIME_SLOT_" + id + "_slotName", if missing falls back to slotNameKey
                String translatedSlotName = translations.getOrDefault(translationKey, slot.getSlotNameKey());
                
                dailySlots.add(new SlotStatus(
                        Long.valueOf(slot.getId()),
                        translatedSlotName,
                        slot.getStartTime(),
                        slot.getEndTime(),
                        "AVAILABLE",
                        true,
                        null
                ));
            }
            availabilityMap.put(date, dailySlots);
        }

        // 4. Fetch overlapping active bookings
        List<BookingStatus> blockingStatuses = Arrays.asList(BookingStatus.APPROVED, BookingStatus.PENDING);
        List<Booking> overlappingBookings = bookingRepository.findBookingsByClassroomAndDateRange(
                classroomId, startDate, endDate, blockingStatuses
        );

        // 5. Mutate skeleton to mark occupied slots
        for (Booking booking : overlappingBookings) {
            LocalDate bDate = booking.getBookingDate();
            List<SlotStatus> daySlots = availabilityMap.get(bDate);
            if (daySlots != null) {
                booking.getBookingTimeSlots().forEach(bts -> {
                    Long bSlotId = Long.valueOf(bts.getTimeSlot().getId());
                    // Find and mutate the specific slot
                    for (int i = 0; i < daySlots.size(); i++) {
                        SlotStatus current = daySlots.get(i);
                        if (current.slotId().equals(bSlotId)) {
                            daySlots.set(i, new SlotStatus(
                                    current.slotId(),
                                    current.slotName(),
                                    current.startTime(),
                                    current.endTime(),
                                    booking.getStatus().name(),
                                    false,
                                    booking.getId() // Required to inform admin UI/frontend about occupant
                            ));
                            break;
                        }
                    }
                });
            }
        }

        // 6. Assemble Final Output
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

    @Transactional(readOnly = true)
    @Override
    public Map<Long, DateAvailability> getBulkClassroomsAvailabilityForDate(List<Long> classroomIds, LocalDate date) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Fetch available timeslots
        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        allSlots.sort(Comparator.comparing(TimeSlot::getStartTime));

        // 2. Fetch translations for timeslots
        Set<Long> slotIds = allSlots.stream()
                .map(s -> Long.valueOf(s.getId()))
                .collect(Collectors.toSet());
        Map<TranslatableEntityType, Set<Long>> typeMap = new HashMap<>();
        typeMap.put(TranslatableEntityType.TIME_SLOT, slotIds);
        Map<String, String> translations = translationService.getTranslations(typeMap);

        // 3. Initialize mapping skeleton: map every classroom_id to a full daily grid
        Map<Long, DateAvailability> bulkMap = new HashMap<>();
        for (Long cId : classroomIds) {
            List<SlotStatus> dailySlots = new ArrayList<>();
            for (TimeSlot slot : allSlots) {
                String translationKey = TranslationKeyBuilder.build(TranslatableEntityType.TIME_SLOT, Long.valueOf(slot.getId()), "name");
                String translatedSlotName = translations.getOrDefault(translationKey, slot.getSlotNameKey());
                
                dailySlots.add(new SlotStatus(
                        Long.valueOf(slot.getId()),
                        translatedSlotName,
                        slot.getStartTime(),
                        slot.getEndTime(),
                        "AVAILABLE",
                        true,
                        null
                ));
            }
            bulkMap.put(cId, new DateAvailability(date, dailySlots));
        }

        // 4. Fetch overlapping active bookings for ALL requested rooms on that date
        List<BookingStatus> blockingStatuses = Arrays.asList(BookingStatus.APPROVED, BookingStatus.PENDING);
        List<Booking> overlappingBookings = bookingRepository.findBookingsByClassroomIdsAndDate(
                classroomIds, date, blockingStatuses
        );

        // 5. Mutate skeletons to mark occupied slots mapped safely by Room ID
        for (Booking booking : overlappingBookings) {
            Long cId = booking.getClassroom().getId();
            DateAvailability classroomDateGrid = bulkMap.get(cId);
            if (classroomDateGrid != null) {
                List<SlotStatus> daySlots = classroomDateGrid.slots();
                
                booking.getBookingTimeSlots().forEach(bts -> {
                    Long bSlotId = Long.valueOf(bts.getTimeSlot().getId());
                    for (int i = 0; i < daySlots.size(); i++) {
                        SlotStatus current = daySlots.get(i);
                        if (current.slotId().equals(bSlotId)) {
                            daySlots.set(i, new SlotStatus(
                                    current.slotId(),
                                    current.slotName(),
                                    current.startTime(),
                                    current.endTime(),
                                    booking.getStatus().name(),
                                    false,
                                    booking.getId()
                            ));
                            break;
                        }
                    }
                });
            }
        }

        return bulkMap;
    }
}
