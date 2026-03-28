package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.LoginRequest;
import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.AuthResponse;
import com.thang.roombooking.common.enums.AuthStatus;
import com.thang.roombooking.common.enums.UserRole;
import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.entity.ExternalIdentity;
import com.thang.roombooking.entity.RefreshToken;
import com.thang.roombooking.entity.Role;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.repository.RoleRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.AuthService;
import com.thang.roombooking.service.RefreshTokenService;
import com.thang.roombooking.service.TokenBlacklistService;
import com.thang.roombooking.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final GoogleOAuth2AuthenticationService googleOAuth2AuthenticationService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("{} | Login request: {}", LogConstant.ACTION_START, request);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
            );

            UserAccount user = null;

            if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof SecurityUserDetails userDetails) {
                user = userDetails.getUser();
            }

            String accessToken = tokenService.generateAccessToken(user);
            String refreshToken = tokenService.generateRefreshToken();
            refreshTokenService.saveRefreshToken(user, refreshToken);
            if (user == null) {
                throw new AppException(CommonErrorCode.INTERNAL_ERROR, "User not found in authentication context");
            }
            log.info("{} | Login successful: {}", LogConstant.ACTION_SUCCESS, request);
            return buildAuthResponse(accessToken, refreshToken, user);
        } catch (DisabledException _) {
            throw new AppException(AuthErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException _) {
            throw new AppException(AuthErrorCode.ACCOUNT_LOCKED);
        } catch (BadCredentialsException _) {
            throw new AppException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email/username
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(AuthErrorCode.USER_ALREADY_EXISTS, request.getEmail());
        }
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(AuthErrorCode.USER_ALREADY_EXISTS, request.getUsername());
        }

        Role studentRole = roleRepository.findByName(UserRole.STUDENT.name())
                .orElseThrow(() -> new AppException(AuthErrorCode.ROLE_NOT_FOUND, UserRole.STUDENT.name()));

        UserAccount newUser = UserAccount.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(studentRole)
                .status(UserStatus.ACTIVE)
                .build();

        newUser = userAccountRepository.save(newUser);

        // Auto-login after registration
        String accessToken = tokenService.generateAccessToken(newUser);
        String refreshToken = tokenService.generateRefreshToken();
        refreshTokenService.saveRefreshToken(newUser, refreshToken);

        return buildAuthResponse(accessToken, refreshToken, newUser);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        log.info("{} | Refresh token : {}", LogConstant.ACTION_START, rawRefreshToken);
        try {
            if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
                throw new AppException(AuthErrorCode.TOKEN_INVALID);
            }

            RefreshToken currentToken = refreshTokenService.verifyRefreshToken(rawRefreshToken);
            UserAccount user = currentToken.getUser();

            // Always generate a new access token
            String newAccessToken = tokenService.generateAccessToken(user);

            // Rotate refresh token if it's close to expiry (< 3 days remaining)
            String finalRefreshToken = rawRefreshToken;
            long daysUntilExpiry = Duration.between(Instant.now(), currentToken.getExpiryDate()).toDays();

            if (daysUntilExpiry <= 3) {
                String newRefreshToken = tokenService.generateRefreshToken();
                refreshTokenService.revokeRefreshToken(rawRefreshToken);
                refreshTokenService.saveRefreshToken(user, newRefreshToken);
                finalRefreshToken = newRefreshToken;
            }

            return buildAuthResponse(newAccessToken, finalRefreshToken, user);
        } catch (AppException e) {
            log.warn("{} | Refresh token failed | Error : {}", LogConstant.BIZ_ERROR, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Refresh token failed | System Error.", LogConstant.BIZ_ERROR, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Logout requested with empty refresh token");
            return;
        }
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        log.info("{} | Logout requested.", LogConstant.ACTION_START);
        try {
            if (accessToken == null || accessToken.isBlank()) {
                log.warn("Access token is null or blank during logout. Skipping blacklist.");
                return;
            } else {
                long remainingSeconds = tokenService.getRemainingTimeInSeconds(accessToken);
                if (remainingSeconds > 0) {
                    tokenBlacklistService.blacklistToken(accessToken, remainingSeconds);
                }
            }
            refreshTokenService.revokeRefreshToken(refreshToken);
        } catch (AppException e) {
            log.warn("{} | Logout failed: {}", LogConstant.ACTION_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("{} | Logout failed | System Error.", LogConstant.SYS_ERROR, e);
        }
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, UserAccount user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().getName())
                .status(AuthStatus.LOGIN_SUCCESS)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        log.info("{} | Google login", LogConstant.ACTION_START);
        try {
            ExternalIdentity identity = googleOAuth2AuthenticationService.getUserInfo(idToken);
            String email = identity.email();
            log.info("Google identity verified. Email: {}", email);

            UserAccount user = userAccountRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(AuthErrorCode.ACCOUNT_DOES_NOT_EXISTS));

            if (user.getStatus() == UserStatus.BANNED) {
                throw new AppException(AuthErrorCode.ACCOUNT_DISABLED);
            }

            String accessToken = tokenService.generateAccessToken(user);
            String refreshToken = tokenService.generateRefreshToken();
            refreshTokenService.saveRefreshToken(user, refreshToken);
            log.info("{} | Google identity verified.", LogConstant.ACTION_SUCCESS);
            return buildAuthResponse(accessToken, refreshToken, user);
        } catch (AppException e) {
            log.warn("{} | Google login failed: | Error : {}", LogConstant.ACTION_FAILED, e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("{} | Error in login with google | System Error. ", LogConstant.SYS_ERROR, e);
            throw new AppException(CommonErrorCode.INTERNAL_ERROR, "Google login failed");
        }
    }
}
