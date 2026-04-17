package com.thang.roombooking.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String baseUrl;
    private String frontendUrl;

    /**
     * Brand/app metadata used across outbound communications (email templates, links, etc.).
     * Defaults are intentionally generic so this module can be reused across projects.
     */
    private String name = "Room Booking";
    private String tagline = "Room Booking System";
    private String url = "http://localhost:8080";

    private Support support = new Support();
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Support {
        private String email = "support@localhost";
    }

    @Getter
    @Setter
    public static class Mail {
        /**
         * Optional override for the visible \"From\" address.
         * If unset, the service can fall back to spring.mail.username.
         */
        private String fromEmail;

        /**
         * Optional admin inbox for alerts.
         */
        private String adminEmail;

        private Social social = new Social();
    }

    @Getter
    @Setter
    public static class Social {
        private String instagramUrl;
        private String facebookUrl;
        private String pinterestUrl;
        private String twitterUrl;
    }
}
