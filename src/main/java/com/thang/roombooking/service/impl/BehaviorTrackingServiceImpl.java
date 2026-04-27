package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.enums.BehaviorEventType;
import com.thang.roombooking.repository.UserBehaviorRepository;
import com.thang.roombooking.service.BehaviorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BehaviorTrackingServiceImpl implements BehaviorTrackingService {

    private final UserBehaviorRepository userBehaviorRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(Long userId, BehaviorEventType eventType,
                            String entityType, Long entityId, String metadata) {
        try {
            userBehaviorRepository.insertEvent(userId, eventType.name(), entityType, entityId, metadata);
        } catch (Exception e) {
            log.warn("Skipped behavior event [{}/{}] for user {}: {}", eventType, entityId, userId, e.getMessage());
        }
    }

    @Override
    public void recordEvent(Long userId, BehaviorEventType eventType, String entityType, Long entityId) {
        recordEvent(userId, eventType, entityType, entityId, null);
    }
}
