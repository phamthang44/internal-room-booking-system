package com.thang.roombooking.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    // Lưu trữ bucket trong RAM (Map <Key, Bucket>)
    // Key ở đây sẽ là Email của user
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, this::createNewBucket);
    }

    private Bucket createNewBucket(String key) {
        // Cấu hình: Cho phép tối đa 3 request trong vòng 5 phút
        // Refill: Sau 5 phút sẽ bơm đầy lại 3 token
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(5)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Hàm xóa bucket (dùng khi user nhập đúng OTP thì reset lại cho họ)
    public void resetBucket(String key) {
        cache.remove(key);
    }
}
