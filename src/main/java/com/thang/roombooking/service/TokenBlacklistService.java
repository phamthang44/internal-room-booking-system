package com.thang.roombooking.service;

public interface TokenBlacklistService {
    void blacklistToken(String token, long timeToLiveSeconds);
    boolean isTokenBlacklisted(String token);
}
