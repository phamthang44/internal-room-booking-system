package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.CreateClassroomRequest;
import com.thang.roombooking.common.dto.request.UpdateClassroomRequest;
import com.thang.roombooking.common.dto.response.CreateClassroomResponse;
import com.thang.roombooking.common.dto.response.UpdateClassroomResponse;

public interface ClassroomCommandService {

    CreateClassroomResponse createAnClassroom(CreateClassroomRequest req);
    UpdateClassroomResponse updateClassroom(UpdateClassroomRequest req);
}
