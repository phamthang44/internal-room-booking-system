package com.thang.roombooking.common.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationTrendResponse {
    LocalDate date;
    String violationType;
    long violationCount;
    long totalSeverityPts;
}
