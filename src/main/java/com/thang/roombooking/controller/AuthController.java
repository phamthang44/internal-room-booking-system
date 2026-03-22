package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.request.LoginRequest;
import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.AuthResponse;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.AuthErrorCode;
import com.thang.roombooking.common.utils.CookieUtils;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @PostMapping("/login")
    public ResponseEntity<ApiResult<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        log.info("Login attempt for: {}", request.getIdentifier());
        AuthResponse authResult = authService.login(request);
        setRefreshTokenCookie(response, authResult.refreshToken());
        return ResponseEntity.ok(ApiResult.success(authResult, I18nUtils.get("message.logged_in")));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResult<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse authResult = authService.register(request);
        setRefreshTokenCookie(response, authResult.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(authResult, I18nUtils.get("user.add.success")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResult<AuthResponse>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = CookieUtils.getCookieValue(request, REFRESH_TOKEN_COOKIE);

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(AuthErrorCode.TOKEN_INVALID, I18nUtils.get("error.token_invalid"));
        }

        AuthResponse authResult = authService.refreshToken(rawRefreshToken);
        setRefreshTokenCookie(response, authResult.refreshToken());
        return ResponseEntity.ok(ApiResult.success(authResult, I18nUtils.get("user.upd.success")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = CookieUtils.getCookieValue(request, REFRESH_TOKEN_COOKIE);

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
            // You can add logic to revoke the access token if needed
        }

        authService.logout(accessToken, rawRefreshToken);

        // Clear cookie
        ResponseCookie cleared = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)  //dev env
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cleared.toString());
        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
