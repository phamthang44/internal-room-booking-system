package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.service.AnalyticsSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSnapshotJob {

    private final AnalyticsSnapshotService analyticsSnapshotService;

    /**
     * Refreshes all pre-computed analytics snapshots nightly at 1:00 AM UTC.
     * Recomputes the last 7 days to catch any late-arriving data, plus
     * current and previous week for room utilization.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void refreshNightlySnapshots() {
        log.info("{} | Analytics snapshot refresh started", LogConstant.ACTION_START);

        try {
            LocalDate today     = LocalDate.now(ZoneOffset.UTC);
            LocalDate sevenAgo  = today.minusDays(7);
            LocalDate thisMonday = today.with(DayOfWeek.MONDAY);

            analyticsSnapshotService.refreshDailySnapshots(sevenAgo, today);
            analyticsSnapshotService.refreshViolationDailySnapshots(sevenAgo, today);
            analyticsSnapshotService.refreshRoomUtilizationSnapshots(thisMonday);
            analyticsSnapshotService.refreshRoomUtilizationSnapshots(thisMonday.minusWeeks(1));

            log.info("{} | Analytics snapshot refresh completed", LogConstant.ACTION_SUCCESS);
        } catch (Exception e) {
            log.error("{} | Analytics snapshot refresh failed", LogConstant.SYS_ERROR, e);
        }
    }
}
