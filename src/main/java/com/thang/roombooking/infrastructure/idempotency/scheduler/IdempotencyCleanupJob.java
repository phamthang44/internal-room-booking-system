package com.thang.roombooking.infrastructure.idempotency.scheduler;


import com.thang.roombooking.infrastructure.idempotency.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private final IdempotencyRepository repository;

    @Scheduled(cron = "0 0 2 * * ?") // Chạy vào 2 giờ sáng mỗi ngày
    @Transactional
    public void cleanExpiredKeys() {
        repository.deleteByExpiresAtBefore(Instant.now());
        log.info("Đã dọn dẹp các Idempotency keys hết hạn.");
    }
}
