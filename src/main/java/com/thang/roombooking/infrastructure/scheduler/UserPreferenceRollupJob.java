package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.enums.BehaviorEventType;
import com.thang.roombooking.repository.UserBehaviorRepository;
import com.thang.roombooking.repository.UserRoomPreferenceScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceRollupJob {

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserRoomPreferenceScoreRepository preferenceScoreRepository;

    private static final int RETENTION_DAYS = 90;

    /**
     * Recomputes user-room preference scores from raw behavior events.
     * Runs nightly at 2:00 AM UTC, after the analytics snapshot job at 1:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void rollupPreferenceScores() {
        log.info("{} | User preference rollup started", LogConstant.ACTION_START);

        try {
            Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);

            List<Object[]> rows = userBehaviorRepository.aggregateClassroomEvents(cutoff);

            // Accumulate counts per (userId, classroomId).
            // Array layout: [0]=userId, [1]=classroomId, [2]=views, [3]=clicks, [4]=dismisses
            record RoomKey(long userId, long classroomId) {}
            Map<RoomKey, int[]> counters = new HashMap<>();

            for (Object[] row : rows) {
                long userId      = (Long) row[0];
                long classroomId = (Long) row[1];
                BehaviorEventType type = (BehaviorEventType) row[2];
                long count       = (Long) row[3];

                RoomKey key = new RoomKey(userId, classroomId);
                int[] acc = counters.computeIfAbsent(key, k -> new int[3]);

                switch (type) {
                    case ROOM_VIEWED              -> acc[0] += (int) count;
                    case RECOMMENDATION_CLICKED   -> acc[1] += (int) count;
                    case RECOMMENDATION_DISMISSED -> acc[2] += (int) count;
                    default -> { /* other event types not yet mapped to classroom scores */ }
                }
            }

            // Clear stale scores and insert freshly computed ones (all in one transaction).
            preferenceScoreRepository.deleteAllScores();

            for (Map.Entry<RoomKey, int[]> entry : counters.entrySet()) {
                RoomKey key = entry.getKey();
                int[] acc = entry.getValue();
                int views = acc[0], clicks = acc[1], dismisses = acc[2];

                double score = (views * 0.5) + (clicks * 2.0) - (dismisses * 4.0);
                preferenceScoreRepository.insertScore(key.userId(), key.classroomId(),
                        views, clicks, dismisses, score);
            }

            // Purge raw events beyond retention window.
            int purged = userBehaviorRepository.deleteEventsBefore(cutoff);

            log.info("{} | Preference rollup: {} scores computed, {} old events purged",
                    LogConstant.ACTION_SUCCESS, counters.size(), purged);

        } catch (Exception e) {
            log.error("{} | User preference rollup failed", LogConstant.SYS_ERROR, e);
        }
    }
}
