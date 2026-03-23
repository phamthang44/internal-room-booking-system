package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.AuthErrorCode;
import com.thang.roombooking.common.validator.AccountAuthenticationValidator;
import com.thang.roombooking.entity.Role;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.common.enums.IdentityProvider;
import com.thang.roombooking.repository.RoleRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

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

    @Override
    @Transactional
    public UserBasicResponse updateUserStatus(Long userId, UserStatus userStatus) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));
        
        user.setStatus(userStatus);
        userAccountRepository.save(user);
        log.info("User {} status updated to {}", userId, userStatus);
        
        return UserBasicResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserBasicResponse updateUserEmail(Long userId, String email) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

        if (!AccountAuthenticationValidator.isValidEmail(email)) {
            throw new AppException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (userAccountRepository.existsByEmail(email) && !user.getEmail().equalsIgnoreCase(email)) {
            throw new AppException(AuthErrorCode.EMAIL_ALREADY_EXISTS, email);
        }

        user.setEmail(email.trim().toLowerCase());
        userAccountRepository.save(user);
        log.info("User {} email updated to {}", userId, email);

        return UserBasicResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserBasicResponse updateFullName(Long userId, String fullName) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

        if (!AccountAuthenticationValidator.isValidFullName(fullName)) {
            throw new AppException(AuthErrorCode.INVALID_FULLNAME_FORMAT);
        }

        user.setFullName(fullName.trim());
        userAccountRepository.save(user);
        log.info("User {} full name updated to {}", userId, fullName);

        return UserBasicResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserBasicResponse updatePassword(Long userId, String password) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

        if (!AccountAuthenticationValidator.isValidPassword(password)) {
            throw new AppException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }

        user.setPassword(passwordEncoder.encode(password));
        userAccountRepository.save(user);
        log.info("User {} password updated", userId);

        return UserBasicResponse.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBasicResponse createAnAccount(RegisterRequest req) {
        log.info("Admin creating new account with username: {}, email: {}", req.getUsername(), req.getEmail());

        validateAccountDetails(req);

        checkAccountExistence(req);

        Role defaultRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND, "STUDENT"));

        UserAccount user = UserAccount.builder()
                .username(req.getUsername().trim().toLowerCase())
                .fullName(req.getFullName().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail().trim().toLowerCase())
                .role(defaultRole)
                .status(UserStatus.ACTIVE)
                .provider(IdentityProvider.LOCAL)
                .build();

        userAccountRepository.save(user);
        log.info("Successfully created account for user ID: {}", user.getId());

        return UserBasicResponse.fromEntity(user);
    }

    private void validateAccountDetails(RegisterRequest req) {
        if (!AccountAuthenticationValidator.isValidEmail(req.getEmail())) {
            throw new AppException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }
        if (!AccountAuthenticationValidator.isValidFullName(req.getFullName())) {
            throw new AppException(AuthErrorCode.INVALID_FULLNAME_FORMAT);
        }
        if (!AccountAuthenticationValidator.isValidPassword(req.getPassword())) {
            throw new AppException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (!AccountAuthenticationValidator.validatePassword(req.getPassword(), req.getConfirmPassword())) {
            throw new AppException(AuthErrorCode.PASSWORDS_DO_NOT_MATCH);
        }
    }

    private void checkAccountExistence(RegisterRequest req) {
        if (userAccountRepository.existsByUsername(req.getUsername())) {
            throw new AppException(AuthErrorCode.USERNAME_ALREADY_EXISTS, req.getUsername());
        }
        if (userAccountRepository.existsByEmail(req.getEmail())) {
            throw new AppException(AuthErrorCode.EMAIL_ALREADY_EXISTS, req.getEmail());
        }
    }
}
