package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.UserBasicResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<UserBasicResponse> getAllUsers(Pageable pageable);
    void banUser(Long userId);
    UserBasicResponse updateUserRole(Long userId, String roleName);
}
