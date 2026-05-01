package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.UserProfileResponse;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserQueryServiceImpl implements UserQueryService {

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(SecurityUserDetails currentUser) {

        var user = currentUser.getUser();

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .id(user.getId())
                .fullName(user.getFullName())
                .roleName(user.getRole().getName())
                .studentCode(user.getStudentCode())
                .build();
    }
}
