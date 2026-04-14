package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.StudentDashboardResponse;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students") // Hoặc /api/v1/me nếu là người dùng hiện tại
@RequiredArgsConstructor
public class StudentDashboardController {

    private final BookingQueryService bookingQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<StudentDashboardResponse>> getDashboard(@AuthenticationPrincipal SecurityUserDetails currentUser) {
        return ResponseEntity.ok(
                ApiResult.success(bookingQueryService.getStudentDashboard(currentUser.getUser().getId()))
        );
    }
}