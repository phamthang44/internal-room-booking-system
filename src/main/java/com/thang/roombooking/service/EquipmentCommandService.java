package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.CreateEquipmentRequest;
import com.thang.roombooking.common.dto.request.UpdateEquipmentRequest;
import com.thang.roombooking.common.dto.response.AdminEquipmentDetailResponse;

public interface EquipmentCommandService {
    AdminEquipmentDetailResponse createEquipment(CreateEquipmentRequest req);
    AdminEquipmentDetailResponse updateEquipment(Integer id, UpdateEquipmentRequest req);
    void deactivateEquipment(Integer id);
    void reactivateEquipment(Integer id);
}
