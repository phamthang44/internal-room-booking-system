package com.thang.roombooking.infrastructure.configuration;

import com.thang.roombooking.common.enums.PenaltyAction;
import com.thang.roombooking.common.enums.ViolationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "penalty.rules")
public class PenaltyProperties {

    private int windowDays;
    private int cancellationSpamThreshold = 5;
    private Map<ViolationType, Integer> points;
    private List<Threshold> thresholds;

    @Getter
    @Setter
    public static class Threshold {
        private int minPoints;
        private PenaltyAction action;
        private Integer durationDays;
    }
}
