package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.UserBasicResponse;

import com.thang.roombooking.common.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<UserBasicResponse> getAllUsers(Pageable pageable);
    void banUser(Long userId);
    UserBasicResponse updateUserRole(Long userId, String roleName);


    UserBasicResponse updateUserStatus(Long userId, UserStatus userStatus);

    UserBasicResponse updateUserEmail(Long userId, String email);

    UserBasicResponse updateFullName(Long userId, String phone);

    UserBasicResponse updatePassword(Long userId, String password);

    UserBasicResponse createAnAccount(RegisterRequest req);
}
