package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.AuthResponse;
import com.thang.roombooking.common.enums.AuthStatus;
import com.thang.roombooking.common.enums.IdentityProvider;
import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.AuthErrorCode;
import com.thang.roombooking.entity.ExternalIdentity;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.OAuthService;
import com.thang.roombooking.service.RefreshTokenService;
import com.thang.roombooking.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthServiceImpl implements OAuthService {

    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse loginWithGoogle(ExternalIdentity externalIdentity) {
        String email = externalIdentity.email();

        // 1. Kiểm tra xem User đã tồn tại chưa
        Optional<UserAccount> existingUser = userAccountRepository.findByEmail(email);
        UserAccount user;
        if (existingUser.isPresent()) {
            // --- CASE EXISTING USER ---
            user = existingUser.get();
            if (user.getStatus() == UserStatus.INACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
            }
            if (user.getStatus() == UserStatus.BANNED) {
                throw new AppException(AuthErrorCode.ACCOUNT_DISABLED, "Account is disabled. Please contact administrator.");
            }
            
            // Có thể update provider nếu muốn đánh dấu user này đã link Google
            if (user.getProvider() != IdentityProvider.GOOGLE) {
                user.setProvider(IdentityProvider.GOOGLE);
                user.setProviderId(externalIdentity.externalUserId());
                userAccountRepository.save(user);
            }

        } else {
            // --- CASE DO NOT EXIST ---
            log.warn("Unauthorized Google login attempt for email: {}", email);
            throw new AppException(AuthErrorCode.USER_NOT_FOUND, "User not found or not allowed. Please contact administrator.");
        }
        // 2. Tạo Token (Tái sử dụng TokenService)
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken();
        refreshTokenService.saveRefreshToken(user, refreshToken);

        return generateAuthResponse(accessToken, refreshToken, user);
    }

    private AuthResponse generateAuthResponse(String accessToken, String refreshToken, UserAccount user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().getName())
                .status(AuthStatus.LOGIN_SUCCESS)
                .build();
    }
}
