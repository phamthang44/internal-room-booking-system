package com.thang.roombooking.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Allows raw WebSocket connections (ws://localhost:8080/api/chat) from frontend
        registry.addEndpoint("/api/chat")
                .setAllowedOriginPatterns("*");

        // Allows SockJS fallback over HTTP (http://localhost:8080/api/chat)
        registry.addEndpoint("/api/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        // Allows raw WebSocket connections explicitly at /ws (which the frontend actually calls)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
                
        // Also support SockJS at /ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker and set the destination prefixes
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}   