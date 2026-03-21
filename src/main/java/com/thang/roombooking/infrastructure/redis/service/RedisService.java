package com.thang.roombooking.infrastructure.redis.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {

    void setValue(String key, String value, long duration, TimeUnit unit);

    String getValue(String key);

    void deleteKey(String key);

    boolean exists(String key);
}
