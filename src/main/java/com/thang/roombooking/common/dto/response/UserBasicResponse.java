package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.entity.UserAccount;

public record UserBasicResponse(Long id, String username, String email, String role, String status, String studentCode) {

    public UserBasicResponse(Long id, String username, String email, String role, String status) {
        this(id, username, email, role, status, null);
    }

    public static UserBasicResponse fromEntity(UserAccount user) {
        return new UserBasicResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName(),
                user.getStatus().name()
        );
    }

    public static UserBasicResponse fromEntityWithStudentCode(UserAccount user) {
        return new UserBasicResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName(),
                user.getStatus().name(),
                user.getStudentCode()
        );
    }
}
