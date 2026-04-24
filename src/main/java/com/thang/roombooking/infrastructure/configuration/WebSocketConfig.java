package com.thang.roombooking.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] DOMAIN_URLS = new String[]{
            "https://internal-room-booking-system-fronte.vercel.app",
            "https://www.roomhub.online"
    };

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Allows raw WebSocket connections explicitly at /ws (which the frontend actually calls)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(DOMAIN_URLS);
                
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker and set the destination prefixes
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}   