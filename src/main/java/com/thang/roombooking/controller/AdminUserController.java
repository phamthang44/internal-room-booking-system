package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.service.AdminUserService;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<Page<UserBasicResponse>>> getAllUsers(Pageable pageable) {
        Page<UserBasicResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResult.success(users, I18nUtils.get("user.profile.retrieve.success")));
    }

    @PutMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<Void>> banUser(@PathVariable Long userId) {
        adminUserService.banUser(userId);
        return ResponseEntity.ok(ApiResult.success(null, I18nUtils.get("user.change.success")));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<UserBasicResponse>> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String roleName) {
            
        UserBasicResponse response = adminUserService.updateUserRole(userId, roleName);
        return ResponseEntity.ok(ApiResult.success(response, I18nUtils.get("user.upd.success")));
    }
}
