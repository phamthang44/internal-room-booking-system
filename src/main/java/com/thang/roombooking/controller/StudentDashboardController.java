package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.RoomRecommendationResponse;
import com.thang.roombooking.common.dto.response.StudentDashboardResponse;
import com.thang.roombooking.common.enums.BehaviorEventType;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.BehaviorTrackingService;
import com.thang.roombooking.service.BookingQueryService;
import com.thang.roombooking.service.StudentRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final BookingQueryService bookingQueryService;
    private final StudentRecommendationService studentRecommendationService;
    private final BehaviorTrackingService behaviorTrackingService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResult<StudentDashboardResponse>> getDashboard(
            @AuthenticationPrincipal SecurityUserDetails currentUser) {
        return ResponseEntity.ok(
                ApiResult.success(bookingQueryService.getStudentDashboard(currentUser.getUser().getId()))
        );
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResult<List<RoomRecommendationResponse>>> getRecommendations(
            @AuthenticationPrincipal SecurityUserDetails currentUser,
            @RequestParam(required = false) Integer attendees,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                ApiResult.success(studentRecommendationService.getRecommendations(
                        currentUser.getUser().getId(), attendees, date))
        );
    }

    @PostMapping("/recommendations/{classroomId}/click")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResult<Void>> trackRecommendationClick(
            @AuthenticationPrincipal SecurityUserDetails currentUser,
            @PathVariable Long classroomId) {
        behaviorTrackingService.recordEvent(currentUser.getUser().getId(),
                BehaviorEventType.RECOMMENDATION_CLICKED, "CLASSROOM", classroomId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    @PostMapping("/recommendations/{classroomId}/dismiss")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResult<Void>> trackRecommendationDismiss(
            @AuthenticationPrincipal SecurityUserDetails currentUser,
            @PathVariable Long classroomId) {
        behaviorTrackingService.recordEvent(currentUser.getUser().getId(),
                BehaviorEventType.RECOMMENDATION_DISMISSED, "CLASSROOM", classroomId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}