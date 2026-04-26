package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.*;
import com.thang.roombooking.common.enums.RoomSort;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.BuildingMapper;
import com.thang.roombooking.common.mapper.ClassroomMapper;
import com.thang.roombooking.common.mapper.EquipmentMapper;
import com.thang.roombooking.common.mapper.RoomTypeMapper;
import com.thang.roombooking.common.search.ClassroomFields;
import com.thang.roombooking.common.search.GenericSpecificationBuilder;
import com.thang.roombooking.common.search.SearchOperation;
import com.thang.roombooking.common.enums.AssetType;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.ClassroomEquipment;
import com.thang.roombooking.entity.RoomAsset;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.service.AvailabilityService;
import com.thang.roombooking.service.ClassroomQueryService;
import com.thang.roombooking.service.TranslationService;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassroomQueryServiceImpl implements ClassroomQueryService {

    private final ClassroomRepository classroomRepository;
    private final TranslationService translationService;
    private final ClassroomMapper classroomMapper;
    private final BuildingMapper buildingMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final EquipmentMapper equipmentMapper;
    private final AvailabilityService availabilityService;


    private Map<TranslatableEntityType, Set<Long>> handleEntityIdsByType(Page<Classroom> classrooms) {
        Map<TranslatableEntityType, Set<Long>> idsByType = new EnumMap<>(TranslatableEntityType.class);

        for (Classroom c : classrooms.getContent()) {
            // Building ID
            populateEntityIdsFromClassroom(c, idsByType);

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
    @Transactional(readOnly = true)
    public Page<ClassroomListResponse> searchPublic(RoomSearchRequest req) {
        //req.setRoomStatus(RoomStatus.AVAILABLE);

        if (req.getBookingDate() == null) req.setBookingDate(LocalDate.now());
        // Note: no default slot injection — null timeSlotIds means "show all slots, no filter"

        Specification<Classroom> spec = buildSpecification(req);
        Pageable pageable = buildPageable(req);

        Page<Classroom> classrooms = classroomRepository.findAll(spec, pageable);

        // 1. Collect entity IDs classified by type for bulk translation
        Map<TranslatableEntityType, Set<Long>> idsByType = handleEntityIdsByType(classrooms);

        // 2. Bulk-translate building / roomType / equipment names
        Map<String, String> translations = translationService.getTranslations(idsByType);

        // 3. Fetch bulk availability — avoids N+1 queries
        List<Long> classroomIds = classrooms.getContent().stream().map(Classroom::getId).toList();
        Map<Long, DateAvailability> bulkSchedule = availabilityService.getBulkClassroomsAvailabilityForDate(classroomIds, req.getBookingDate());

        // Snapshot queried slot IDs for use in the lambda (must be effectively final)
        List<Integer> queriedSlotIds = (req.getTimeSlotIds() != null && !req.getTimeSlotIds().isEmpty())
                ? req.getTimeSlotIds()
                : List.of();

        return classrooms.map(c -> {
            ClassroomListResponse res = classroomMapper.toBasicResponse(c, translations);
            DateAvailability dailySchedule = bulkSchedule.get(c.getId());
            res.setDailySchedule(dailySchedule);

            if (!queriedSlotIds.isEmpty() && dailySchedule != null && dailySchedule.slots() != null) {
                // Extract only the slots the user cares about
                List<SlotStatus> queriedSlotsStatus = dailySchedule.slots().stream()
                        .filter(s -> queriedSlotIds.contains(s.slotId().intValue()))
                        .toList();

                long availableCount = queriedSlotsStatus.stream().filter(SlotStatus::isAvailable).count();

                res.setQueriedSlotsStatus(queriedSlotsStatus);
                res.setAvailableSlotCount((int) availableCount);
                res.setTotalQueriedSlots(queriedSlotIds.size());
                // A room is "available for query" only when ALL requested slots are free
                res.setAvailableForQuery(availableCount == queriedSlotIds.size());
            } else {
                // No slot filter applied: mark as available if any slot is free in the day
                boolean anyFree = dailySchedule != null && dailySchedule.slots() != null
                        && dailySchedule.slots().stream().anyMatch(SlotStatus::isAvailable);
                res.setAvailableForQuery(anyFree);
                res.setQueriedSlotsStatus(List.of());
                res.setAvailableSlotCount(0);
                res.setTotalQueriedSlots(0);
            }

            return res;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDetailClassroomResponse getClassroomDetail(Long id) {

        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Classroom ID: " + id));

        Map<String, String> translations = buildTranslations(classroom);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);
        ClassroomAvailabilityResponse schedule = availabilityService.getClassroomAvailability(id, startDate, endDate);

        return buildAdminDetailResponse(classroom, translations, schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public DetailClassroomResponse getDetailClassroom(Long id) {

        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, "Classroom ID: " + id));

        Map<String, String> translations = buildTranslations(classroom);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(6);
        ClassroomAvailabilityResponse schedule = availabilityService.getClassroomAvailability(id, startDate, endDate);

        if (classroom.getStatus() != RoomStatus.AVAILABLE) {
            List<DateAvailability> modifiedAvailabilities = schedule.availabilities().stream()
                    .map(da -> new DateAvailability(
                            da.date(),
                            da.slots().stream()
                                    .map(slot -> new SlotStatus(
                                            slot.slotId(), slot.slotName(), slot.startTime(), slot.endTime(), slot.status(), false, slot.currentBookingId(), slot.rejectionReason(), slot.isMine()
                                    )).toList()

                    )).toList();
            schedule = new ClassroomAvailabilityResponse(schedule.date(), schedule.isFull(), modifiedAvailabilities);
        }

        return buildClientDetailResponse(classroom, translations, schedule);
    }


    private Map<String, String> buildTranslations(Classroom classroom) {

        Map<TranslatableEntityType, Set<Long>> ids = new EnumMap<>(TranslatableEntityType.class);

        // Building
        populateEntityIdsFromClassroom(classroom, ids);

        // Equipments
        if (classroom.getClassroomEquipments() != null) {
            Set<Long> equipmentIds = classroom.getClassroomEquipments().stream()
                    .filter(e -> e.getEquipment() != null)
                    .map(e -> Long.valueOf(e.getEquipment().getId()))
                    .collect(Collectors.toSet());

            ids.put(TranslatableEntityType.EQUIPMENT, equipmentIds);
        }

        return translationService.getTranslations(ids);
    }

    private static void populateEntityIdsFromClassroom(Classroom classroom, Map<TranslatableEntityType, Set<Long>> ids) {
        if (classroom.getBuilding() != null) {
            ids.computeIfAbsent(TranslatableEntityType.BUILDING, k -> new HashSet<>())
                    .add(classroom.getBuilding().getId());
        }

        // RoomType
        if (classroom.getRoomType() != null) {
            ids.computeIfAbsent(TranslatableEntityType.ROOM_TYPE, k -> new HashSet<>())
                    .add(classroom.getRoomType().getId());
        }
    }


    private DetailClassroomResponse buildClientDetailResponse(Classroom classroom,  Map<String, String> translations, ClassroomAvailabilityResponse schedule) {
        List<RoomAssetResponse> roomAssets = mapRoomAssets(classroom);
        List<String> imageUrls = deriveImageUrls(roomAssets);
        return DetailClassroomResponse.builder()
                .classroomId(classroom.getId())
                .building(buildingMapper.toBasicBuildingResponse(
                        classroom.getBuilding(), translations))
                .roomName(classroom.getRoomName())
                .capacity(classroom.getCapacity())
                .status(classroom.getStatus())
                .schedule(schedule)
                .equipments(classroom.getClassroomEquipments() == null ? List.of() : 
                        classroom.getClassroomEquipments().stream()
                        .map(e -> equipmentMapper.toEquipmentResponse(e, translations))
                        .toList())
                .addressBuildingLocation(
                        classroom.getBuilding() != null
                                ? classroom.getBuilding().getAddress()
                                : null
                )
                .roomType(roomTypeMapper.toBasicRoomTypeResponse(
                        classroom.getRoomType(), translations))
                .roomAssets(roomAssets)
                .imageUrls(imageUrls)
                .build();
    }

    private AdminDetailClassroomResponse buildAdminDetailResponse(
            Classroom classroom,
            Map<String, String> translations,
            ClassroomAvailabilityResponse schedule
    ) {
        List<RoomAssetResponse> roomAssets = mapRoomAssets(classroom);
        List<String> imageUrls = deriveImageUrls(roomAssets);

        return AdminDetailClassroomResponse.builder()
                .building(buildingMapper.toBasicBuildingResponse(
                        classroom.getBuilding(), translations))
                .roomName(classroom.getRoomName())
                .capacity(classroom.getCapacity())
                .availableDates(getAvailableDates(classroom.getId()))
                .schedule(schedule)
                .equipments(classroom.getClassroomEquipments().stream()
                        .map(e -> equipmentMapper.toEquipmentResponse(e, translations))
                        .toList())
                .addressBuildingLocation(
                        classroom.getBuilding() != null
                                ? classroom.getBuilding().getAddress()
                                : null
                )
                .roomType(roomTypeMapper.toBasicRoomTypeResponse(
                        classroom.getRoomType(), translations))
                .roomAssets(roomAssets)
                .imageUrls(imageUrls)
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

    private List<RoomAssetResponse> mapRoomAssets(Classroom classroom) {
        if (classroom.getRoomAssets() == null || classroom.getRoomAssets().isEmpty()) return List.of();

        return classroom.getRoomAssets().stream()
                .map(this::toRoomAssetResponse)
                .sorted(Comparator
                        .comparing((RoomAssetResponse a) -> a.isPrimary() != null && a.isPrimary())
                        .reversed()
                        .thenComparing(a -> a.id() == null ? Long.MAX_VALUE : a.id()))
                .toList();
    }

    private RoomAssetResponse toRoomAssetResponse(RoomAsset asset) {
        return RoomAssetResponse.builder()
                .id(asset.getId())
                .url(asset.getUrl())
                .assetType(asset.getAssetType())
                .isPrimary(asset.getIsPrimary())
                .build();
    }

    private List<String> deriveImageUrls(List<RoomAssetResponse> roomAssets) {
        if (roomAssets == null || roomAssets.isEmpty()) return List.of();
        return roomAssets.stream()
                .filter(a -> a.assetType() == null || a.assetType() == AssetType.IMAGE)
                .map(RoomAssetResponse::url)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Instant> getAvailableDates(Long classroomId) {
        return List.of(); // TODO query booking / schedule
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

        // ── Availability filtering ────────────────────────────────────────────────
        // We intentionally do NOT filter rooms at the SQL/Spec level here.
        // AvailabilityServiceImpl.getBulkClassroomsAvailabilityForDate() already
        // calls BookingRepository.findBookingsByClassroomIdsAndDate() and stamps
        // each room's slots with the correct SlotBookingStatus (OCCUPIED, PENDING,
        // IN_USE, etc.).  The mapping step in searchPublic() then derives:
        //   • queriedSlotsStatus  – per-slot breakdown for the requested slot IDs
        //   • isAvailableForQuery – true only when ALL requested slots are free
        //   • availableSlotCount  – "2/3 slots available" counter for FE badges
        //
        // This design intentionally returns ALL rooms with their availability state
        // so the FE can render unavailable rooms as greyed-out/locked cards rather
        // than hiding them entirely (better UX for room discovery).
        //
        // If strict server-side pre-filtering is required in the future (e.g. an
        // ?onlyAvailable=true flag), the correct approach is:
        //   1. Add a lightweight query to BookingRepository:
        //      findBookedClassroomIdsForDateAndSlots(date, slotIds, activeStatuses)
        //   2. Pass the result into the spec as: root.get("id").not().in(bookedIds)
        //   Note: this keeps the AvailabilityServiceImpl logic unchanged and avoids
        //   duplicating the booking-overlap logic in a JPA Criteria subquery.

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
