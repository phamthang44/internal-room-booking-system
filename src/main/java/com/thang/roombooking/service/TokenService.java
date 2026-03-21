package com.thang.roombooking.service;

import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;

public interface TokenService {
    String generateAccessToken(UserAccount userAccount);
    String generateRefreshToken();
    String extractUsername(String token);
    boolean isValid(String token, SecurityUserDetails userDetails);
    long getRemainingTimeInSeconds(String token);
}
