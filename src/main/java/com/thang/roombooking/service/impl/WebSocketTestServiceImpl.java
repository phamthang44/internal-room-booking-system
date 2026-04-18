package com.thang.roombooking.service.impl;

import com.thang.roombooking.service.WebSocketTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestServiceImpl implements WebSocketTestService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendTestMessage(String message) {
        log.info("Sending message to /topic/test: {}", message);
        messagingTemplate.convertAndSend("/topic/test", message);
    }
}
