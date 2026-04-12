package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.RoomSearchRequest;
import com.thang.roombooking.common.dto.response.AdminDetailClassroomResponse;
import com.thang.roombooking.common.dto.response.ClassroomListResponse;
import com.thang.roombooking.common.dto.response.DetailClassroomResponse;
import org.springframework.data.domain.Page;

public interface ClassroomQueryService {

    Page<ClassroomListResponse> searchPublic(RoomSearchRequest request);

    AdminDetailClassroomResponse getClassroomDetail(Long id);

    DetailClassroomResponse getDetailClassroom(Long id);
}
