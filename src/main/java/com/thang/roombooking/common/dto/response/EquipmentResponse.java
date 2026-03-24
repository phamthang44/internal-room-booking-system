package com.thang.roombooking.common.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EquipmentResponse {
    private int id;
    private String name;
    private String description;
}
