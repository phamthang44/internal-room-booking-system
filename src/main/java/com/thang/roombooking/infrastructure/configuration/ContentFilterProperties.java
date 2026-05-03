package com.thang.roombooking.infrastructure.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "content-filter")
public class ContentFilterProperties {
    private List<String> badWords;

    private static List<String> staticBadWords = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (badWords != null) {
            staticBadWords = new ArrayList<>(badWords);
        }
    }

    public static List<String> getStaticBadWords() {
        return staticBadWords;
    }
}
