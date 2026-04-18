package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyUser(String userId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                new NotificationPayload(type, message, null, null, null, null)
        );
    }

    @Override
    public void notifyUser(String userId, NotificationPayload payload) {
        log.info("Sending WS notification to user {} | type={} | bookingId={}",
                userId, payload.type(), payload.bookingId());
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                payload
        );
    }
}
