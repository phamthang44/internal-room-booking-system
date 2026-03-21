package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.AuthErrorCode;
import com.thang.roombooking.entity.Role;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.RoleRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserBasicResponse> getAllUsers(Pageable pageable) {
        return userAccountRepository.findAll(pageable)
                .map(UserBasicResponse::fromEntity);
    }

    @Override
    @Transactional
    public void banUser(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(AuthErrorCode.USER_ALREADY_BANNED, userId);
        }

        if (user.getRole().getName().equalsIgnoreCase("ADMIN")) {
            throw new AppException(AuthErrorCode.CANNOT_BAN_ADMIN, userId);
        }

        user.setStatus(UserStatus.BANNED);
        userAccountRepository.save(user);
        log.info("User {} has been banned", userId);
    }

    @Override
    @Transactional
    public UserBasicResponse updateUserRole(Long userId, String roleName) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));
                
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND, roleName));
                
        user.setRole(role);
        userAccountRepository.save(user);
        log.info("User {} role updated to {}", userId, roleName);
        
        return UserBasicResponse.fromEntity(user);
    }
}
