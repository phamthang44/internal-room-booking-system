package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.UserBasicResponse;

import java.util.List;

public interface AdminUserService {
    List<UserBasicResponse> getAllUsers();
    void banUser(Long userId);
    UserBasicResponse updateUserRole(Long userId, String roleName);
}
