package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.LoginRequest;
import com.thang.roombooking.common.dto.request.RegisterRequest;
import com.thang.roombooking.common.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse refreshToken(String rawRefreshToken);
    void logout(String refreshToken);
    void logout(String accessToken, String refreshToken);
}
