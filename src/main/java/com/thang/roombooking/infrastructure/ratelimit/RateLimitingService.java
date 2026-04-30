package com.thang.roombooking.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;

public interface RateLimitingService {
    Bucket resolveBucket(String key);
}
