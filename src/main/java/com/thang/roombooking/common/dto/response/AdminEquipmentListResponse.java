package com.thang.roombooking.common.dto.response;

public record AdminEquipmentListResponse(
        Integer id,
        String nameKey,
        String nameVi,
        String nameEn,
        boolean isActive,
        long classroomCount
) {}
