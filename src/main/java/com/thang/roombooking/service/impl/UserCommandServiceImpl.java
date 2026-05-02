package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.UpdateProfileRequest;
import com.thang.roombooking.common.dto.response.UserProfileResponse;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.UserCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandServiceImpl implements UserCommandService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("{} | Update profile | userId: {}", LogConstant.ACTION_START, userId);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(request.phoneNumber());
        userAccountRepository.save(user);

        log.info("{} | Update profile success | userId: {}", LogConstant.ACTION_SUCCESS, userId);

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleName(user.getRole().getName())
                .studentCode(user.getStudentCode())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
