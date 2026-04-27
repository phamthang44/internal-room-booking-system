package com.thang.roombooking.service;

import com.thang.roombooking.common.enums.BehaviorEventType;

public interface BehaviorTrackingService {

    void recordEvent(Long userId, BehaviorEventType eventType, String entityType, Long entityId, String metadata);

    void recordEvent(Long userId, BehaviorEventType eventType, String entityType, Long entityId);
}
