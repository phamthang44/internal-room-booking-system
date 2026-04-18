package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.service.WebSocketTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ws-test")
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {

    private final WebSocketTestService webSocketTestService;

    // HTTP Endpoint to trigger sending a WebSocket message from the server-side
    @PostMapping("/send")
    public ResponseEntity<ApiResult<String>> sendTestMessage(@RequestParam String message) {
        log.info("HTTP Request received to send WS message: {}", message);
        webSocketTestService.sendTestMessage(message);
        return ResponseEntity.ok(ApiResult.success("Message sent successfully to /topic/test: " + message));
    }

    // Direct WebSocket message handling (listening on /app/test)
    @MessageMapping("/test")
    @SendTo("/topic/test")
    public String handleWebSocketMessage(String message) {
        log.info("WebSocket Client message received on /app/test: {}", message);
        return "Server echo: " + message;
    }
}
