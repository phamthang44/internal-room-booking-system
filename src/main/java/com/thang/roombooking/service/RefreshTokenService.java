package com.thang.roombooking.service;

import com.thang.roombooking.entity.RefreshToken;
import com.thang.roombooking.entity.UserAccount;

public interface RefreshTokenService {
    RefreshToken saveRefreshToken(UserAccount user, String tokenString);
    RefreshToken verifyRefreshToken(String token);
    void revokeRefreshToken(String token);
    void revokeAllUserTokens(UserAccount user);
}
