package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.AdminEquipmentDetailResponse;
import com.thang.roombooking.common.dto.response.AdminEquipmentListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EquipmentQueryService {
    Page<AdminEquipmentListResponse> listEquipment(String keyword, Pageable pageable);
    AdminEquipmentDetailResponse getEquipment(Integer id);
}
