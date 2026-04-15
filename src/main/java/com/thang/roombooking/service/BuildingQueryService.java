package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.AdminBuildingResponse;

import java.util.List;

public interface BuildingQueryService {

    List<AdminBuildingResponse> retrieveAllBuildings();

}
