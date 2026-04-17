package com.thang.roombooking.infrastructure.storage.cloudinary;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudinary")
@Getter
public class CloudinaryProperties {
    @Value("${cloudinary.cloudName}")
    private String cloudName;

    @Value("${cloudinary.apiKey}")
    private String apiKey;

    @Value("${cloudinary.apiSecret}")
    private String apiSecret;

    /**
     * Logical folder for uploads (project-specific).
     */
    @Value("${cloudinary.folder:room-booking-uploads}")
    private String folder;
}
