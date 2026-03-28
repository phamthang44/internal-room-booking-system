package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
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
        log.info("{} | Banning account User | {}", LogConstant.ACTION_START, userId);
        try {
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
            log.info("{} | Ban success userId: {}", LogConstant.ACTION_SUCCESS, userId);
        } catch (AppException e) {
            log.warn("{} | Banning account User | ID: {}, Error: {}", LogConstant.BIZ_ERROR, userId, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Banning account User | ID: {}", LogConstant.SYS_ERROR, userId, e);
            throw e;
        }
        log.info("User {} has been banned", userId);
    }

    @Override
    @Transactional
    public UserBasicResponse updateUserRole(Long userId, String roleName) {
        log.info("{} | Update user role for userId: {}, role: {}", LogConstant.ACTION_START, userId, roleName);
        try {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND, roleName));

            user.setRole(role);
            userAccountRepository.save(user);
            log.info("{} | Update success | userId: {} | role: {}", LogConstant.ACTION_SUCCESS, userId, roleName);
            return UserBasicResponse.fromEntity(user);
        } catch (AppException e) {
            log.warn("{} | Update Role | Error {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Update Role | User id: {}", LogConstant.SYS_ERROR, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public UserBasicResponse updateUserStatus(Long userId, UserStatus userStatus) {
        log.info("{} | Update user status for userId: {}, status: {}", LogConstant.ACTION_START, userId, userStatus.toString());
        try {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

            user.setStatus(userStatus);
            userAccountRepository.save(user);
            log.info("{} | Update user status for userId: {}, status: {}", LogConstant.ACTION_START, userId, userStatus);
            return UserBasicResponse.fromEntity(user);
        } catch (AppException e) {
            log.warn("{} | Update Status | Error {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Update Status | User id: {}", LogConstant.SYS_ERROR, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public UserBasicResponse updateUserEmail(Long userId, String email) {
        log.info("{} | Update Email for userId: {}, email: {}", LogConstant.ACTION_START, userId, email );
        try {
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

            log.info("{} | Update user email successfully | user id : {}", LogConstant.ACTION_SUCCESS, userId);

            return UserBasicResponse.fromEntity(user);
        } catch (AppException e) {
            log.warn("{} | Update Email | User id: {}", LogConstant.BIZ_ERROR, userId, e);
            throw e;
        } catch (Exception e) {
            log.error("{} | Update Email | User id: {}", LogConstant.SYS_ERROR, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public UserBasicResponse updateFullName(Long userId, String fullName) {
        log.info("{} | Update user full name  for userId: {}, fullName: {}", LogConstant.ACTION_START, userId, fullName);
        try {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

            if (!AccountAuthenticationValidator.isValidFullName(fullName)) {
                throw new AppException(AuthErrorCode.INVALID_FULLNAME_FORMAT);
            }

            user.setFullName(fullName.trim());
            userAccountRepository.save(user);

            log.info("{} | Update user full name successfully | userId: {}, fullName: {}", LogConstant.ACTION_SUCCESS, userId, fullName);

            return UserBasicResponse.fromEntity(user);
        } catch (AppException e) {
            log.warn("{} | Update full name | User id: {}", LogConstant.BIZ_ERROR, userId, e);
            throw e;
        } catch (Exception e) {
            log.error("{} | Update full name | User id: {}", LogConstant.SYS_ERROR, userId, e);
            throw e;
        }

    }

    @Override
    @Transactional
    public UserBasicResponse updatePassword(Long userId, String password) {
        log.info("{} | Update password for userId: {}, password: {}", LogConstant.ACTION_START, userId, password);
        try {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, userId));

            if (!AccountAuthenticationValidator.isValidPassword(password)) {
                throw new AppException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
            }

            user.setPassword(passwordEncoder.encode(password));
            userAccountRepository.save(user);
            log.info("{} | Update password successfully | userId: {}", LogConstant.ACTION_SUCCESS, userId);
            return UserBasicResponse.fromEntity(user);
        } catch (AppException e) {
            log.warn("{} | Update password | User id: {}", LogConstant.BIZ_ERROR, userId, e);
            throw e;
        }  catch (Exception e) {
            log.error("{} | Update password | User id: {}", LogConstant.SYS_ERROR, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBasicResponse createAnAccount(RegisterRequest req) {
        log.info("{} | Create an user account: {}", LogConstant.ACTION_START, req);
        try {
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

            UserAccount savedUser = userAccountRepository.save(user);
            log.info("{} | Create account successfully | user id: {}", LogConstant.ACTION_SUCCESS, savedUser.getId());
            return UserBasicResponse.fromEntity(user);
        } catch (AppException e) {
            log.warn("{} | Create account | Error : {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Create account | Error : {}", LogConstant.SYS_ERROR, e.getMessage());
            throw e;
        }
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
