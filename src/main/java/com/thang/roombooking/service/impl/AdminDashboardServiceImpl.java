package com.thang.roombooking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.dto.response.AdminDashboardOverviewResponse;
import com.thang.roombooking.common.dto.response.RecentViolationSummary;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.infrastructure.redis.service.RedisService;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String CACHE_KEY = "admin:dashboard:overview";
    private static final long CACHE_TTL_SECONDS = 300L;

    private final BookingRepository bookingRepository;
    private final ClassroomRepository classroomRepository;
    private final PenaltyRecordRepository penaltyRecordRepository;
    private final BookingViolationRepository bookingViolationRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardOverviewResponse getOverview() {
        String cached = redisService.getValue(CACHE_KEY);
        if (cached != null) {
            log.debug("admin:dashboard:overview cache hit");
            try {
                return objectMapper.readValue(cached, AdminDashboardOverviewResponse.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize dashboard cache, recomputing", e);
            }
        }

        AdminDashboardOverviewResponse response = buildOverview();

        try {
            redisService.setValue(CACHE_KEY, objectMapper.writeValueAsString(response), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache dashboard overview", e);
        }

        return response;
    }

    private AdminDashboardOverviewResponse buildOverview() {
        List<Object[]> statusRows = bookingRepository.countGroupedByStatus();
        Map<String, Long> breakdown = new HashMap<>();
        long pendingCount = 0L;
        for (Object[] row : statusRows) {
            BookingStatus status = (BookingStatus) row[0];
            Long count = (Long) row[1];
            breakdown.put(status.name(), count);
            if (status == BookingStatus.PENDING) {
                pendingCount = count;
            }
        }

        long bookingsToday = bookingRepository.countByBookingDate(LocalDate.now());
        long activeRooms = classroomRepository.countActiveByStatus(RoomStatus.AVAILABLE);
        long activePenalties = penaltyRecordRepository.countActivePenalties();

        List<BookingViolation> recentRaw = bookingViolationRepository.findRecentViolations(PageRequest.of(0, 5));
        List<RecentViolationSummary> recentViolations = recentRaw.stream()
                .map(v -> RecentViolationSummary.builder()
                        .violationId(v.getId())
                        .userEmail(v.getUser().getEmail())
                        .studentCode(v.getUser().getStudentCode())
                        .violationType(v.getType().name())
                        .severityPoints(v.getSeverityPoints())
                        .createdAt(v.getCreatedAt())
                        .build())
                .toList();

        return AdminDashboardOverviewResponse.builder()
                .pendingApprovalCount(pendingCount)
                .bookingsTodayCount(bookingsToday)
                .activeRoomsCount(activeRooms)
                .activePenaltyCount(activePenalties)
                .bookingStatusBreakdown(breakdown)
                .recentViolations(recentViolations)
                .build();
    }
}
