package com.thang.roombooking.common.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBuildingResponse {
    private Long id;
    private String buildingName;
    private String address;
    private AuditResponse auditResponse;
    private boolean isActive;
}
