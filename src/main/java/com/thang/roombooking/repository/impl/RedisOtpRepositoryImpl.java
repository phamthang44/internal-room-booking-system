package com.thang.roombooking.repository.impl;


import com.thang.roombooking.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisOtpRepositoryImpl implements OtpCodeRepository {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String email, String otpCode, long ttlMinutes) {
        String key = "otp:" + email; // Prefix chuẩn
        redisTemplate.opsForValue().set(key, otpCode, ttlMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String findByEmail(String email) {
        return redisTemplate.opsForValue().get("otp:" + email);
    }

    @Override
    public void deleteByEmail(String email) {
        redisTemplate.delete("otp:" + email);
    }
}
