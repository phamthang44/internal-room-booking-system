package com.thang.roombooking.common.dto.response;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BasicRoomTypeResponse {
    private Long id;
    private String name;
}
