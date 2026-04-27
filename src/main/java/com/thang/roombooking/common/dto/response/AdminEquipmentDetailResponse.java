package com.thang.roombooking.common.dto.response;

public record AdminEquipmentDetailResponse(
        Integer id,
        String nameKey,
        String nameVi,
        String nameEn,
        String descVi,
        String descEn,
        boolean isActive,
        long classroomCount,
        AuditResponse audit
) {}
