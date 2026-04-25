package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.event.ViolationCreatedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.configuration.PenaltyProperties;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.infrastructure.configuration.RoomBookingRabbitMQProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.common.constant.RabbitMQConstants;
import com.thang.roombooking.common.dto.model.NotificationPayload;
import com.thang.roombooking.common.dto.request.InAppNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PenaltyEnforcementListener {

    private final PenaltyProperties penaltyProperties;
    private final BookingViolationRepository violationRepository;
    private final PenaltyRecordRepository penaltyRepository;
    private final UserAccountRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RoomBookingRabbitMQProperties rabbitMQProperties;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    @Transactional
    public void handleViolationCreated(ViolationCreatedEvent event) {
        log.info("{} | Evaluating penalties for user {} after violation {}", LogConstant.ACTION_START, event.getUserId(), event.getViolationId());

        UserAccount user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, event.getUserId()));

        Instant cutoffTime = Instant.now().minus(penaltyProperties.getWindowDays(), ChronoUnit.DAYS);
        List<BookingViolation> recentViolations = violationRepository.findByUserIdAndCreatedAtAfter(user.getId(), cutoffTime);

        int totalPoints = recentViolations.stream()
                .mapToInt(v -> v.getSeverityPoints() != null ? v.getSeverityPoints() : 0)
                .sum();

        log.info("User {} has {} severity points in the last {} days", user.getId(), totalPoints, penaltyProperties.getWindowDays());

        // Find the highest threshold that is met
        PenaltyProperties.Threshold appliedThreshold = penaltyProperties.getThresholds().stream()
                .filter(t -> totalPoints >= t.getMinPoints())
                .max(Comparator.comparingInt(PenaltyProperties.Threshold::getMinPoints))
                .orElse(null);

        if (appliedThreshold != null) {
            log.info("Threshold met: applying {}", appliedThreshold.getAction());
            
            // Check if user already has an active penalty of this level to avoid duplicate records
            boolean alreadyApplied = penaltyRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                    .anyMatch(p -> p.getPenaltyAction() == appliedThreshold.getAction());

            if (alreadyApplied) {
                log.info("Penalty {} is already active for user {}. Skipping creation.", appliedThreshold.getAction(), user.getId());
                return;
            }
            
            Instant endTime = null;
            if (appliedThreshold.getDurationDays() != null) {
                endTime = Instant.now().plus(appliedThreshold.getDurationDays(), ChronoUnit.DAYS);
            }

            // Store a structured JSON or delimited string so the frontend/API can translate it dynamically
            // Alternatively, if you want it translated immediately on the server, you could use I18nUtils
            String reasonPayload = String.format("{\"key\": \"penalty.reason.accumulated_points\", \"args\": [%d, %d]}", 
                    totalPoints, penaltyProperties.getWindowDays());

            PenaltyRecord penalty = PenaltyRecord.builder()
                    .user(user)
                    .penaltyAction(appliedThreshold.getAction())
                    .isActive(true)
                    .startDate(Instant.now())
                    .endDate(endTime)
                    .reason(reasonPayload)
                    .build();

            penaltyRepository.save(penalty);
            
            // Optionally update the violation to link to this penalty
            violationRepository.findById(event.getViolationId()).ifPresent(v -> {
                v.setPenalty(penalty);
                violationRepository.save(v);
            });

            // --- RABBITMQ INTEGRATION: Send In-App Notification ---
            try {
                NotificationPayload payload = new NotificationPayload(
                        "PENALTY_APPLIED",
                        "Penalty Applied", // Fallback text
                        "Your account has been penalized.", // Fallback text
                        null, // No specific booking ID linked to the penalty overall
                        "URGENT",
                        Instant.now(),
                        "notification.penalty.applied", // Fixed prefix
                        new Object[]{
                                translateReason(reasonPayload), 
                                I18nUtils.get("penalty.action." + appliedThreshold.getAction().name().toLowerCase())
                        }
                );

                InAppNotificationRequest notificationRequest = new InAppNotificationRequest(user.getId(), payload);

                rabbitTemplate.convertAndSend(
                        rabbitMQProperties.getExchange(),
                        RabbitMQConstants.RK_NOTIFICATION_IN_APP,
                        notificationRequest
                );
                log.info("Sent penalty notification to RabbitMQ for user {}", user.getId());
            } catch (Exception e) {
                log.error("Failed to send penalty notification to RabbitMQ", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String translateReason(String reason) {
        if (reason == null || !reason.startsWith("{")) {
            return reason;
        }
        try {
            // objectMapper is likely not available here, but I can use standard I18nUtils 
            // if I have a similar helper or just add it.
            // Wait, I should add ObjectMapper to this listener too.
            Map<String, Object> map = objectMapper  .readValue(reason, Map.class);
            String key = (String) map.get("key");
            List<Object> args = (List<Object>) map.get("args");
            if (key != null) {
                return I18nUtils.get(key, args != null ? args.toArray() : new Object[0]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse penalty reason in listener: {}", reason);
        }
        return reason;
    }
}
