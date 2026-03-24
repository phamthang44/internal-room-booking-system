package com.thang.roombooking.seeder;

import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.entity.Role;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.RoleRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run this seeder first
public class UserSeeder implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userAccountRepository.count() == 0) {
            log.info("Starting UserAccount seeding...");

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
            Role staffRole = roleRepository.findByName("STAFF")
                    .orElseThrow(() -> new RuntimeException("STAFF role not found"));
            Role studentRole = roleRepository.findByName("STUDENT")
                    .orElseThrow(() -> new RuntimeException("STUDENT role not found"));

            List<UserAccount> usersToSeed = List.of(
                    UserAccount.builder()
                            .username("admin_thang")
                            .fullName("Phạm Đức Thắng")
                            .email("thang.admin@univ.edu.vn")
                            .password(passwordEncoder.encode("admin123"))
                            .role(adminRole)
                            .status(UserStatus.ACTIVE)
                            .build(),
                    UserAccount.builder()
                            .username("staff_lan")
                            .fullName("Nguyễn Thị Lan")
                            .email("lan.staff@univ.edu.vn")
                            .password(passwordEncoder.encode("staff123"))
                            .role(staffRole)
                            .status(UserStatus.ACTIVE)
                            .build(),
                    UserAccount.builder()
                            .username("student_nam")
                            .fullName("Lâm Nguyễn Trung Nam")
                            .email("nam.student@univ.edu.vn")
                            .password(passwordEncoder.encode("student123"))
                            .role(studentRole)
                            .status(UserStatus.ACTIVE)
                            .build()
            );

            userAccountRepository.saveAll(usersToSeed);
            log.info("Successfully seeded 3 user accounts with BCrypt encrypted passwords!");
        } else {
            log.info("User accounts already exist - skipping UserSeeder.");
        }
    }
}
