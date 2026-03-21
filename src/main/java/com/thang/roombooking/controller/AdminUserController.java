package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<List<UserBasicResponse>>> getAllUsers() {
        List<UserBasicResponse> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(ApiResult.success(users, "Users retrieved successfully"));
    }

    @PutMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<Void>> banUser(@PathVariable Long userId) {
        adminUserService.banUser(userId);
        return ResponseEntity.ok(ApiResult.success(null, "Ban user successfully"));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<UserBasicResponse>> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {
            
        UserBasicResponse response = adminUserService.updateUserRole(userId, roleName);
        return ResponseEntity.ok(ApiResult.success(response, "User role updated successfully"));
    }
}
