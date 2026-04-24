package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.UserProfileResponse;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;

public interface UserQueryService {

    UserProfileResponse getProfile(SecurityUserDetails currentUser);


}
