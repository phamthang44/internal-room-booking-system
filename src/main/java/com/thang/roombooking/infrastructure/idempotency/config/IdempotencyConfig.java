package com.thang.roombooking.infrastructure.idempotency.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // CỰC KỲ QUAN TRỌNG: Để CleanupJob có thể chạy ngầm
@EnableAspectJAutoProxy // Đảm bảo Spring nhận diện được @Aspect của bạn
public class IdempotencyConfig {

    @Bean
    public ObjectMapper idempotencyObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Hỗ trợ xử lý các kiểu dữ liệu thời gian mới (Java 8 Date/Time)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
