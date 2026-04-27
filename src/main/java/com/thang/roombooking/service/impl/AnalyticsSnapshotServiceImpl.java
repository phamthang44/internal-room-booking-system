package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.DailyBookingTrendResponse;
import com.thang.roombooking.common.dto.response.RoomUtilizationResponse;
import com.thang.roombooking.common.dto.response.ViolationTrendResponse;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.enums.ViolationType;
import com.thang.roombooking.entity.DashboardDailySnapshot;
import com.thang.roombooking.entity.RoomUtilizationSnapshot;
import com.thang.roombooking.entity.ViolationDailySnapshot;
import com.thang.roombooking.repository.*;
import com.thang.roombooking.service.AnalyticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsSnapshotServiceImpl implements AnalyticsSnapshotService {

    private static final List<BookingStatus> COUNTED_STATUSES = List.of(
            BookingStatus.APPROVED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final BookingViolationRepository violationRepository;
    private final ClassroomRepository classroomRepository;
    private final PenaltyRecordRepository penaltyRecordRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DashboardDailySnapshotRepository dailySnapshotRepo;
    private final RoomUtilizationSnapshotRepository roomSnapshotRepo;
    private final ViolationDailySnapshotRepository violationSnapshotRepo;

    // ── Write methods (called by scheduler) ──────────────────────────────────

    @Override
    @Transactional
    public void refreshDailySnapshots(LocalDate from, LocalDate to) {
        long activeRooms = classroomRepository.countActiveByStatus(RoomStatus.AVAILABLE);
        long activePenalties = penaltyRecordRepository.countActivePenalties();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DashboardDailySnapshot snap = dailySnapshotRepo.findBySnapshotDate(date)
                    .orElse(DashboardDailySnapshot.builder().snapshotDate(date).build());

            Map<BookingStatus, Long> statusCounts = toStatusMap(
                    bookingRepository.countByDateGroupedByStatus(date));

            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Map<ViolationType, long[]> violationCounts = toViolationMap(
                    violationRepository.countByDateRangeGroupedByType(dayStart, dayEnd));

            long noShowCount = violationCounts.containsKey(ViolationType.NO_SHOW)
                    ? violationCounts.get(ViolationType.NO_SHOW)[0] : 0L;
            long newViolations = violationCounts.values().stream().mapToLong(v -> v[0]).sum();

            snap.setTotalBookings(statusCounts.values().stream().mapToLong(Long::longValue).sum());
            snap.setPendingCount(statusCounts.getOrDefault(BookingStatus.PENDING, 0L));
            snap.setApprovedCount(statusCounts.getOrDefault(BookingStatus.APPROVED, 0L));
            snap.setRejectedCount(statusCounts.getOrDefault(BookingStatus.REJECTED, 0L));
            snap.setCancelledCount(statusCounts.getOrDefault(BookingStatus.CANCELLED, 0L));
            snap.setCompletedCount(statusCounts.getOrDefault(BookingStatus.COMPLETED, 0L));
            snap.setCheckedInCount(statusCounts.getOrDefault(BookingStatus.CHECKED_IN, 0L));
            snap.setNewViolations(newViolations);
            snap.setNoShowCount(noShowCount);
            snap.setActiveRooms(activeRooms);
            snap.setActivePenalties(activePenalties);
            snap.setComputedAt(Instant.now());

            dailySnapshotRepo.save(snap);
        }
    }

    @Override
    @Transactional
    public void refreshRoomUtilizationSnapshots(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        long totalSlots = timeSlotRepository.count();
        // 5 working days × number of time slots
        int totalSlotCapacity = (int) (5 * totalSlots);

        List<Object[]> rows = bookingRepository.countSlotsByClassroomForRange(
                weekStart, weekEnd, COUNTED_STATUSES);

        for (Object[] row : rows) {
            Long classroomId   = (Long) row[0];
            long totalBookings = (Long) row[1];
            long bookedSlots   = (Long) row[2];
            Double avgAtt      = (Double) row[3];

            BigDecimal utilPct = totalSlotCapacity > 0
                    ? BigDecimal.valueOf(bookedSlots)
                            .divide(BigDecimal.valueOf(totalSlotCapacity), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            classroomRepository.findById(classroomId).ifPresent(classroom -> {
                RoomUtilizationSnapshot snap = roomSnapshotRepo
                        .findByClassroomIdAndWeekStart(classroomId, weekStart)
                        .orElse(RoomUtilizationSnapshot.builder()
                                .classroom(classroom)
                                .weekStart(weekStart)
                                .build());

                snap.setBookedSlotCount((int) bookedSlots);
                snap.setTotalSlotCapacity(totalSlotCapacity);
                snap.setUtilizationPct(utilPct);
                snap.setTotalBookings(totalBookings);
                snap.setAvgAttendees(avgAtt != null
                        ? BigDecimal.valueOf(avgAtt).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
                snap.setComputedAt(Instant.now());

                roomSnapshotRepo.save(snap);
            });
        }
    }

    @Override
    @Transactional
    public void refreshViolationDailySnapshots(LocalDate from, LocalDate to) {
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            Map<ViolationType, long[]> grouped = toViolationMap(
                    violationRepository.countByDateRangeGroupedByType(dayStart, dayEnd));

            for (Map.Entry<ViolationType, long[]> entry : grouped.entrySet()) {
                ViolationType type = entry.getKey();
                long count         = entry.getValue()[0];
                long severityPts   = entry.getValue()[1];

                ViolationDailySnapshot snap = violationSnapshotRepo
                        .findBySnapshotDateAndViolationType(date, type)
                        .orElse(ViolationDailySnapshot.builder()
                                .snapshotDate(date)
                                .violationType(type)
                                .build());

                snap.setViolationCount(count);
                snap.setTotalSeverityPts(severityPts);
                snap.setComputedAt(Instant.now());

                violationSnapshotRepo.save(snap);
            }
        }
    }

    // ── Read methods (called by controller) ──────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DailyBookingTrendResponse> getDailyTrend(int days) {
        LocalDate to   = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(days - 1);
        return dailySnapshotRepo.findBySnapshotDateBetweenOrderBySnapshotDateAsc(from, to)
                .stream()
                .map(s -> DailyBookingTrendResponse.builder()
                        .date(s.getSnapshotDate())
                        .totalBookings(s.getTotalBookings())
                        .pendingCount(s.getPendingCount())
                        .approvedCount(s.getApprovedCount())
                        .rejectedCount(s.getRejectedCount())
                        .cancelledCount(s.getCancelledCount())
                        .completedCount(s.getCompletedCount())
                        .checkedInCount(s.getCheckedInCount())
                        .newViolations(s.getNewViolations())
                        .noShowCount(s.getNoShowCount())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomUtilizationResponse> getRoomStats(int weeks) {
        LocalDate thisMonday = LocalDate.now(ZoneOffset.UTC).with(java.time.DayOfWeek.MONDAY);
        LocalDate from       = thisMonday.minusWeeks(weeks - 1);
        return roomSnapshotRepo.findByWeekStartBetweenOrderByUtilizationPctDesc(from, thisMonday)
                .stream()
                .map(s -> RoomUtilizationResponse.builder()
                        .classroomId(s.getClassroom().getId())
                        .roomName(s.getClassroom().getRoomName())
                        .weekStart(s.getWeekStart())
                        .bookedSlotCount(s.getBookedSlotCount())
                        .totalSlotCapacity(s.getTotalSlotCapacity())
                        .utilizationPct(s.getUtilizationPct())
                        .totalBookings(s.getTotalBookings())
                        .avgAttendees(s.getAvgAttendees())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationTrendResponse> getViolationTrend(int weeks) {
        LocalDate to   = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusWeeks(weeks);
        return violationSnapshotRepo.findBySnapshotDateBetweenOrderBySnapshotDateAsc(from, to)
                .stream()
                .map(s -> ViolationTrendResponse.builder()
                        .date(s.getSnapshotDate())
                        .violationType(s.getViolationType().name())
                        .violationCount(s.getViolationCount())
                        .totalSeverityPts(s.getTotalSeverityPts())
                        .build())
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<BookingStatus, Long> toStatusMap(List<Object[]> rows) {
        Map<BookingStatus, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((BookingStatus) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<ViolationType, long[]> toViolationMap(List<Object[]> rows) {
        Map<ViolationType, long[]> map = new HashMap<>();
        for (Object[] row : rows) {
            ViolationType type = (ViolationType) row[0];
            long count         = (Long) row[1];
            long severityPts   = row[2] != null ? (Long) row[2] : 0L;
            map.put(type, new long[]{count, severityPts});
        }
        return map;
    }
}
