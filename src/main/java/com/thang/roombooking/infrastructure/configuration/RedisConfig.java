package com.thang.roombooking.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    // Spring Boot tự động tạo RedisConnectionFactory từ application.properties
    // Bạn không cần tự define bean LettuceConnectionFactory trừ khi muốn custom sâu.

    // Config này chỉ để đảm bảo Key và Value đều được Serializer dưới dạng String
    // (Mặc định StringRedisTemplate đã làm rồi, nhưng khai báo tường minh vẫn tốt)
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
