package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.BasicClassroomResponse;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.mapper.ClassroomMapper;
import com.thang.roombooking.common.search.GenericSpecificationBuilder;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.repository.TimeSlotRepository;
import com.thang.roombooking.repository.TranslationRepository;
import com.thang.roombooking.service.ClassroomQueryService;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassroomQueryQueryServiceImpl implements ClassroomQueryService {

    private final ClassroomRepository classroomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TranslationRepository translationRepository;
    private final ClassroomMapper classroomMapper;


    private Set<Long> handleEntityIds(Page<Classroom> classrooms) {
        Set<Long> entityIds = new HashSet<>();
        for (Classroom c : classrooms.getContent()) {
            if (c.getBuilding() != null) entityIds.add(c.getBuilding().getId());
            if (c.getClassroomEquipments() != null) {
                for (com.thang.roombooking.entity.ClassroomEquipment ce : c.getClassroomEquipments()) {
                    if (ce.getEquipment() != null) entityIds.add(ce.getEquipment().getId().longValue());
                }
            }
        }
        return entityIds;
    }

    @Override
    public Page<BasicClassroomResponse> searchPublic(RoomSearchRequest req) {
        req.setRoomStatus(RoomStatus.AVAILABLE); //default available for the first time load data

        // 1. Handle defaults for bookingDate
        if (req.getBookingDate() == null) {
            req.setBookingDate(LocalDate.now());
        }

        // 2. Handle defaults for timeSlotId
        if (req.getTimeSlotId() == null) {
            req.setTimeSlotId(4); // Default to time slot ID 4 based on fixed 4 records
        }

        Specification<Classroom> spec = buildSpecification(req);
        Pageable pageable = buildPageable(req);

        Page<Classroom> classrooms = classroomRepository.findAll(spec, pageable);

        Set<Long> entityIds = handleEntityIds(classrooms);

        Map<String, String> translations = new HashMap<>();
        if (!entityIds.isEmpty()) {
            String locale = LocaleContextHolder.getLocale().getLanguage();
            if (locale.isEmpty()) locale = "en";

            List<com.thang.roombooking.entity.Translation> tl = translationRepository.findByEntityTypeInAndEntityIdInAndLocale(
                    List.of("BUILDING", "EQUIPMENT"), new ArrayList<>(entityIds), locale);

            for (com.thang.roombooking.entity.Translation t : tl) {
                translations.put(t.getEntityType() + "_" + t.getEntityId() + "_" + t.getFieldName(), t.getContent());
            }
        }

        return classrooms.map(c -> classroomMapper.toBasicResponse(c, translations));
    }

    /**
     * Builds JPA Specification using GenericSpecificationBuilder
     * Supports: keyword, categoryId, price range, status
     */
    private Specification<Classroom> buildSpecification(RoomSearchRequest req) {
        GenericSpecificationBuilder<Classroom> builder = new GenericSpecificationBuilder<>();

        // Keyword search (roomNumber contains)
        if (StringUtils.hasText(req.getKeyword())) {
            builder.with("roomNumber", ":", req.getKeyword(), "*", "*");
        }

        // Status filter
        if (req.getRoomStatus() != null) {
            builder.with("status", ":", req.getRoomStatus(), null, null);
        }

        if (req.getCapacity() > 0) {
            builder.with("capacity", "≥", req.getCapacity(), null, null);
        }

        // Equipment filter (requires JOIN on classroomEquipments)
        if (req.getEquipmentId() > 0) {
            builder.addSpecification((root, query, criteriaBuilder) -> {
                Join<Object, Object> join = root.join("classroomEquipments", JoinType.INNER);
                return criteriaBuilder.equal(join.get("equipment").get("id"), req.getEquipmentId());
            });
        }

        // Implement booking availability check for bookingDate and timeSlotId 
        // This calculates if overlapping bookings exist for the given timeslot and ignores those rooms.
        //TODO: future implementation of bookingService, and availabilityService for the algo search available date, time slot, overlap as well

        if (req.getBookingDate() != null && req.getTimeSlotId() != null) {
//            TimeSlot slot = timeSlotRepository.findById(req.getTimeSlotId()).orElse(null);
//            if (slot != null && slot.getStartTime() != null && slot.getEndTime() != null) {
//                Instant startInstant = ZonedDateTime.of(req.getBookingDate(), slot.getStartTime(), ZoneId.systemDefault()).toInstant();
//                Instant endInstant = ZonedDateTime.of(req.getBookingDate(), slot.getEndTime(), ZoneId.systemDefault()).toInstant();
//
//                builder.addSpecification((root, query, cb) -> {
//                    Subquery<Long> subquery = query.subquery(Long.class);
//                    Root<Booking> booking = subquery.from(Booking.class);
//                    subquery.select(booking.get("classroom").get("id"));
//
//                    Predicate overlap = cb.and(
//                            cb.lessThan(booking.get("startTime"), endInstant),
//                            cb.greaterThan(booking.get("endTime"), startInstant),
//                            booking.get("status").in(List.of(BookingStatus.PENDING, BookingStatus.APPROVED))
//                    );
//                    subquery.where(overlap);
//
//                    return cb.not(cb.in(root.get("id")).value(subquery));
//                });
//            }
        }

        return builder.build();
    }

    /**
     * Builds Pageable with sorting logic
     * Supports: newest, price_asc, price_desc, name_asc, name_desc
     */
    private Pageable buildPageable(RoomSearchRequest req) {
        // Normalize page number (1-based to 0-based)
        int page = req.getPage();
        if (page >= 1) {
            page = page - 1;
        }

        // Build sort
        Sort sort = buildSort(req.getSort().getValue());

        return PageRequest.of(page, req.getSize(), sort);
    }

    /**
     * Maps sort string to Sort object
     */
    private Sort buildSort(String sortParam) {
        return switch (sortParam != null ? sortParam : "newest") {
            case "capacity_asc" -> Sort.by("capacity").ascending();
            case "capacity_desc" -> Sort.by("capacity").descending();
            case "room_number_asc" -> Sort.by("roomNumber").ascending();
            case "room_number_desc" -> Sort.by("roomNumber").descending();
            default -> Sort.by("createdAt").descending(); // newest
        };
    }
}
