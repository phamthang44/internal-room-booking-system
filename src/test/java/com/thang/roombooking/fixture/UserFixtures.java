package com.thang.roombooking.fixture;

import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.entity.Role;
import com.thang.roombooking.entity.UserAccount;

public final class UserFixtures {

    private UserFixtures() {}

    public static UserAccount studentUser() {
        Role role = Role.builder().id(1L).name("ROLE_STUDENT").build();
        return UserAccount.builder()
                .id(1L)
                .username("student01")
                .email("student01@uni.edu.vn")
                .fullName("Nguyen Van A")
                .password("hashed_password")
                .studentCode("SV001")
                .status(UserStatus.ACTIVE)
                .role(role)
                .version(0)
                .build();
    }

    public static UserAccount adminUser() {
        Role role = Role.builder().id(2L).name("ROLE_ADMIN").build();
        return UserAccount.builder()
                .id(99L)
                .username("admin01")
                .email("admin01@uni.edu.vn")
                .fullName("Admin User")
                .password("hashed_password")
                .studentCode("AD001")
                .status(UserStatus.ACTIVE)
                .role(role)
                .version(0)
                .build();
    }

    public static UserAccount bannedUser() {
        return UserAccount.builder()
                .id(2L)
                .username("banned01")
                .email("banned01@uni.edu.vn")
                .fullName("Nguyen Van B")
                .password("hashed_password")
                .studentCode("SV002")
                .status(UserStatus.BANNED)
                .version(0)
                .build();
    }
}
