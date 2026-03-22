package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.BasicClassroomResponse;
import org.springframework.data.domain.Page;

public interface ClassroomQueryService {

    Page<BasicClassroomResponse> searchPublic(RoomSearchRequest request);



}
