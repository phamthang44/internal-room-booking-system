package com.thang.roombooking.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "roombooking.rabbitmq")
public class RoomBookingRabbitMQProperties {

    private String exchange;
    private Queues queues;
    private RoutingKeys routingKeys;

    @Data
    public static class Queues {
        private String emailPriority;
        private String emailNormal;
        private String emailBooking;
        private String notificationInApp;
    }

    @Data
    public static class RoutingKeys {
        // Binding Patterns
        private String patternEmailPriority;
        private String patternEmailNormal;
        private String patternEmailBooking;
        private String patternNotificationInApp;
    }
}