package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.AdminDetailClassroomResponse;
import com.thang.roombooking.common.dto.response.AuditResponse;
import com.thang.roombooking.common.dto.response.ClassroomListResponse;
import com.thang.roombooking.common.dto.response.TimeSlotResponse;
import com.thang.roombooking.common.enums.RoomSort;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.ClassroomMapper;
import com.thang.roombooking.common.search.ClassroomFields;
import com.thang.roombooking.common.search.GenericSpecificationBuilder;
import com.thang.roombooking.common.search.SearchOperation;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.ClassroomEquipment;
import com.thang.roombooking.entity.Translation;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.repository.TimeSlotRepository;
import com.thang.roombooking.repository.TranslationRepository;
import com.thang.roombooking.service.ClassroomQueryService;
import com.thang.roombooking.service.TranslationService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassroomQueryServiceImpl implements ClassroomQueryService {

    private final ClassroomRepository classroomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TranslationService translationService;
    private final ClassroomMapper classroomMapper;


    private Map<TranslatableEntityType, Set<Long>> handleEntityIdsByType(Page<Classroom> classrooms) {
        Map<TranslatableEntityType, Set<Long>> idsByType = new HashMap<>();

        for (Classroom c : classrooms.getContent()) {
            // Building ID
            if (c.getBuilding() != null) {
                idsByType.computeIfAbsent(TranslatableEntityType.BUILDING, k -> new HashSet<>())
                        .add(c.getBuilding().getId());
            }

            // RoomType ID - Cái này giúp sửa lỗi "room_type.standard_classroom" không dịch được
            if (c.getRoomType() != null) {
                idsByType.computeIfAbsent(TranslatableEntityType.ROOM_TYPE, k -> new HashSet<>())
                        .add(c.getRoomType().getId());
            }

            // Equipment IDs
            if (c.getClassroomEquipments() != null) {
                for (ClassroomEquipment ce : c.getClassroomEquipments()) {
                    if (ce.getEquipment() != null) {
                        idsByType.computeIfAbsent(TranslatableEntityType.EQUIPMENT, k -> new HashSet<>())
                                .add(Long.valueOf(ce.getEquipment().getId()));
                    }
                }
            }
        }
        return idsByType;
    }

    @Override
    public Page<ClassroomListResponse> searchPublic(RoomSearchRequest req) {
        req.setRoomStatus(RoomStatus.AVAILABLE);

        if (req.getBookingDate() == null) req.setBookingDate(LocalDate.now());
        if (req.getTimeSlotId() == null) req.setTimeSlotId(4);

        Specification<Classroom> spec = buildSpecification(req);
        Pageable pageable = buildPageable(req);

        Page<Classroom> classrooms = classroomRepository.findAll(spec, pageable);

        // 1. Thu thập ID đã phân loại theo Type
        Map<TranslatableEntityType, Set<Long>> idsByType = handleEntityIdsByType(classrooms);

        // 2. Gọi service dịch dựa trên Map phân loại (Giải quyết triệt để lỗi trùng ID=7)
        Map<String, String> translations = translationService.getTranslations(idsByType);

        return classrooms.map(c -> classroomMapper.toBasicResponse(c, translations));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDetailClassroomResponse getClassroomDetail(Long id) {

        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Classroom ID: " + id));

        Map<String, String> translations = buildTranslations(classroom);

        return buildAdminDetailResponse(classroom, translations);
    }

    private Map<String, String> buildTranslations(Classroom classroom) {

        Map<TranslatableEntityType, Set<Long>> ids = new HashMap<>();

        // Building
        if (classroom.getBuilding() != null) {
            ids.computeIfAbsent(TranslatableEntityType.BUILDING, k -> new HashSet<>())
                    .add(classroom.getBuilding().getId());
        }

        // RoomType
        if (classroom.getRoomType() != null) {
            ids.computeIfAbsent(TranslatableEntityType.ROOM_TYPE, k -> new HashSet<>())
                    .add(classroom.getRoomType().getId());
        }

        // Equipments
        if (classroom.getClassroomEquipments() != null) {
            Set<?> equipmentIds = classroom.getClassroomEquipments().stream()
                    .map(e -> e.getEquipment().getId())
                    .collect(Collectors.toSet());

            ids.put(TranslatableEntityType.EQUIPMENT, (Set<Long>) equipmentIds);
        }

        return translationService.getTranslations(ids);
    }

    private AdminDetailClassroomResponse buildAdminDetailResponse(
            Classroom classroom,
            Map<String, String> translations
    ) {

        return AdminDetailClassroomResponse.builder()
                .building(classroomMapper.toBasicRoomTypeResponse(
                        classroom.getBuilding(), translations))
                .roomName(classroom.getRoomName())
                .capacity(classroom.getCapacity())
                .availableDates(getAvailableDates(classroom.getId()))
                .month(Instant.now()) // hoặc param truyền vào
                .timeSlots(getTimeSlots(classroom.getId()))
                .equipments(classroom.getClassroomEquipments().stream()
                        .map(e -> classroomMapper.toEquipmentResponse(e, translations))
                        .toList())
                .addressBuildingLocation(
                        classroom.getBuilding() != null
                                ? classroom.getBuilding().getAddress()
                                : null
                )
                .roomType(classroomMapper.toBasicRoomTypeResponse(
                        classroom.getRoomType(), translations))
                .auditResponse(
                        AuditResponse.builder()
                                .createdAt(classroom.getCreatedAt())
                                .updatedAt(classroom.getUpdatedAt())
                                .createdBy(classroom.getCreatedBy())
                                .updatedBy(classroom.getUpdatedBy())
                                .build()
                )
                .build();
    }

    private List<Instant> getAvailableDates(Long classroomId) {
        return List.of(); // TODO query booking / schedule
    }

    private List<TimeSlotResponse> getTimeSlots(Long classroomId) {
        return List.of(); // TODO query schedule
    }

    /**
     * Builds JPA Specification using GenericSpecificationBuilder
     * Supports: keyword, categoryId, price range, status
     */
    private Specification<Classroom> buildSpecification(RoomSearchRequest req) {
        GenericSpecificationBuilder<Classroom> builder = new GenericSpecificationBuilder<>();

        // Keyword search (roomNumber contains)
        if (StringUtils.hasText(req.getKeyword())) {
            builder.with(ClassroomFields.ROOM_NAME, SearchOperation.CONTAINS, req.getKeyword(), null, null);
        }

        // Status filter
        if (req.getRoomStatus() != null) {
            builder.with(ClassroomFields.STATUS, SearchOperation.EQUALITY, req.getRoomStatus(), null, null);
        }

        if (req.getCapacity() > 0) {
            builder.with(ClassroomFields.CAPACITY, SearchOperation.GREATER_THAN_OR_EQUAL, req.getCapacity(), null, null);
        }

        // Equipment filter (requires JOIN on classroomEquipments)
        if (req.getEquipmentId() > 0) {
            builder.addSpecification((root, query, criteriaBuilder) -> {
                Join<Object, Object> join = root.join(ClassroomFields.CLASSROOM_EQUIPMENTS, JoinType.INNER);
                return criteriaBuilder.equal(join.get(ClassroomFields.EQUIPMENT).get(ClassroomFields.ID), req.getEquipmentId());
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
        return switch (sortParam != null ? sortParam : RoomSort.NEWEST.getValue()) {
            case "capacity_asc" -> Sort.by(ClassroomFields.CAPACITY).ascending();
            case "capacity_desc" -> Sort.by(ClassroomFields.CAPACITY).descending();
            case "room_name_asc" -> Sort.by(ClassroomFields.ROOM_NAME).ascending();
            case "room_name_desc" -> Sort.by(ClassroomFields.ROOM_NAME).descending();
            default -> Sort.by(ClassroomFields.CREATED_AT).descending(); // newest
        };
    }
}
