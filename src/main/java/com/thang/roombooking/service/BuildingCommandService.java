package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.CreateBuildingRequest;
import com.thang.roombooking.common.dto.request.UpdateBuildingRequest;
import com.thang.roombooking.common.dto.response.AdminBuildingResponse;

public interface BuildingCommandService {

    AdminBuildingResponse createBuilding(CreateBuildingRequest req);

    AdminBuildingResponse updateBuilding(Long id, UpdateBuildingRequest req);

    void deleteBuilding(Long id);

    AdminBuildingResponse toggleStatus(Long id, boolean isActive);
}
