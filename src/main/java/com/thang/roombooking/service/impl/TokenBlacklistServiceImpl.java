package com.thang.roombooking.service.impl;


import com.thang.roombooking.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:access_token:";

    @Override
    public void blacklistToken(String accessToken, long timeToLiveSeconds) {
        String key = BLACKLIST_PREFIX + accessToken;
        // Lưu vào Redis với thời gian sống = thời gian còn lại của Token
        // Hết thời gian này thì token cũng tự hết hạn, Redis tự xóa key -> Tiết kiệm RAM
        redisTemplate.opsForValue().set(key, "revoked", Duration.ofSeconds(timeToLiveSeconds));
    }

    @Override
    public boolean isTokenBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken));
    }
}
