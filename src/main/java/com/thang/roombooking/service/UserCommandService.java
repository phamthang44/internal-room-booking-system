package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.UpdateProfileRequest;
import com.thang.roombooking.common.dto.response.UserProfileResponse;

public interface UserCommandService {

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

}
