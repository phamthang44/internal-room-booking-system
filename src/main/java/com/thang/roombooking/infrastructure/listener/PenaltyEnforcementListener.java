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
import com.thang.roombooking.common.utils.PenaltyReasonUtils;
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
    private final PenaltyReasonUtils penaltyReasonUtils;

    @Async
    @EventListener
    @Transactional
    public void handleViolationCreated(ViolationCreatedEvent event) {
        log.info("{} | Evaluating penalties for user {} after violation {}", LogConstant.ACTION_START, event.getUserId(), event.getViolationId());

        UserAccount user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, event.getUserId()));

        Instant cutoffTime = Instant.now().minus(penaltyProperties.getWindowDays(), ChronoUnit.DAYS);
        List<BookingViolation> recentViolations = violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(user.getId(), cutoffTime);

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
                log.info("Penalty {} is already active for user {}. Linking new violations to the existing penalty.", appliedThreshold.getAction(), user.getId());
                
                // Still link these violations to one of the active penalties of the same type
                // so they are "consumed" and don't count towards future penalty increments.
                penaltyRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                        .filter(p -> p.getPenaltyAction() == appliedThreshold.getAction())
                        .findFirst()
                        .ifPresent(p -> {
                            recentViolations.forEach(v -> v.setPenalty(p));
                            violationRepository.saveAll(recentViolations);
                        });
                return;
            }
            
            Instant endTime = null;
            if (appliedThreshold.getDurationDays() != null) {
                endTime = Instant.now().plus(appliedThreshold.getDurationDays(), ChronoUnit.DAYS);
            }

            // Store a structured JSON array so the frontend/API can translate it dynamically
            String reasonPayload = penaltyReasonUtils.createInitialReason("penalty.reason.accumulated_points", 
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
            
            // Link all contributing violations to this penalty so they aren't counted again
            recentViolations.forEach(v -> v.setPenalty(penalty));
            violationRepository.saveAll(recentViolations);

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
                                penaltyReasonUtils.translate(reasonPayload), 
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

    private String translateReason(String reason) {
        return penaltyReasonUtils.translate(reason);
    }
}

