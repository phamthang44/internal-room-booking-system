package com.thang.roombooking.common.dto.response;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String username;
    private String roleName;
    private String email;
    private String studentCode;


    //TODO : future fields image url avatar, phone number,
}
