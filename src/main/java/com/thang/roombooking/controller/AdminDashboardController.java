package com.thang.roombooking.controller;

import com.thang.roombooking.common.dto.response.*;
import com.thang.roombooking.service.AdminDashboardService;
import com.thang.roombooking.service.AnalyticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AnalyticsSnapshotService analyticsSnapshotService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<AdminDashboardOverviewResponse>> getOverview() {
        log.info("Admin dashboard overview requested");
        return ResponseEntity.ok(ApiResult.success(adminDashboardService.getOverview()));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<List<DailyBookingTrendResponse>>> getBookingTrend(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Admin dashboard booking trend requested | days={}", days);
        return ResponseEntity.ok(ApiResult.success(analyticsSnapshotService.getDailyTrend(days)));
    }

    @GetMapping("/room-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<List<RoomUtilizationResponse>>> getRoomStats(
            @RequestParam(defaultValue = "4") int weeks) {
        log.info("Admin dashboard room stats requested | weeks={}", weeks);
        return ResponseEntity.ok(ApiResult.success(analyticsSnapshotService.getRoomStats(weeks)));
    }

    @GetMapping("/violation-trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResult<List<ViolationTrendResponse>>> getViolationTrend(
            @RequestParam(defaultValue = "8") int weeks) {
        log.info("Admin dashboard violation trend requested | weeks={}", weeks);
        return ResponseEntity.ok(ApiResult.success(analyticsSnapshotService.getViolationTrend(weeks)));
    }
}
