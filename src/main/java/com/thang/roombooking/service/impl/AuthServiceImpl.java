package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.LoginRequest;
import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.AuthResponse;
import com.thang.roombooking.common.enums.UserRole;
import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.AuthErrorCode;
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

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
            );

            SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
            UserAccount user = userDetails.getUser();

            String accessToken = tokenService.generateAccessToken(user);
            String refreshToken = tokenService.generateRefreshToken();
            refreshTokenService.saveRefreshToken(user, refreshToken);

            return buildAuthResponse(accessToken, refreshToken, user);
        } catch (DisabledException e) {
            throw new AppException(AuthErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            throw new AppException(AuthErrorCode.ACCOUNT_LOCKED);
        } catch (BadCredentialsException e) {
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
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, UserAccount user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().getName())
                .build();
    }
}
