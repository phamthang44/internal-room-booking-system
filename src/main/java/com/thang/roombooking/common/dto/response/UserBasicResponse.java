package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.entity.UserAccount;

public record UserBasicResponse(Long id, String username, String email, String role, String status) {
    public static UserBasicResponse fromEntity(UserAccount user) {
        return new UserBasicResponse(
                user.getId(), 
                user.getUsername(), 
                user.getEmail(), 
                user.getRole().getName(), 
                user.getStatus().name()
        );
    }
}
