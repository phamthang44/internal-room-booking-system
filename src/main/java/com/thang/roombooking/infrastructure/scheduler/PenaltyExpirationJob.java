package com.thang.roombooking.infrastructure.scheduler;
 
import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.Instant;
 
@Component
@RequiredArgsConstructor
@Slf4j
public class PenaltyExpirationJob {
 
    private final PenaltyRecordRepository penaltyRepository;
 
    /**
     * Deactivates expired temporary bans and penalties.
     * Runs every hour at the top of the hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredPenalties() {
        log.info("{} | Cleaning up expired penalties", LogConstant.ACTION_START);
        
        try {
            Instant now = Instant.now();
            int deactivatedCount = penaltyRepository.deactivateExpiredPenalties(now);
            
            if (deactivatedCount > 0) {
                log.info("{} | Successfully deactivated {} expired penalties", LogConstant.ACTION_SUCCESS, deactivatedCount);
            } else {
                log.info("{} | No expired penalties found to deactivate", LogConstant.ACTION_SUCCESS);
            }
            
        } catch (Exception e) {
            log.error("{} | Error during penalty cleanup job", LogConstant.SYS_ERROR, e);
        }
    }
}
