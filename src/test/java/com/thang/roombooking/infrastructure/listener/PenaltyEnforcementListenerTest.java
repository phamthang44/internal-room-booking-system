package com.thang.roombooking.infrastructure.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.enums.PenaltyAction;
import com.thang.roombooking.common.event.ViolationCreatedEvent;
import com.thang.roombooking.common.utils.PenaltyReasonUtils;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.fixture.UserFixtures;
import com.thang.roombooking.infrastructure.configuration.PenaltyProperties;
import com.thang.roombooking.infrastructure.configuration.RoomBookingRabbitMQProperties;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyEnforcementListenerTest {

    @Mock private PenaltyProperties penaltyProperties;
    @Mock private BookingViolationRepository violationRepository;
    @Mock private PenaltyRecordRepository penaltyRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RoomBookingRabbitMQProperties rabbitMQProperties;
    @Mock private ObjectMapper objectMapper;
    @Mock private PenaltyReasonUtils penaltyReasonUtils;

    @InjectMocks
    private PenaltyEnforcementListener listener;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = UserFixtures.studentUser();
        when(penaltyProperties.getWindowDays()).thenReturn(30);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    // ─── below threshold ──────────────────────────────────────────────────────

    @Test
    void should_not_create_penalty_when_total_points_below_all_thresholds() {
        List<BookingViolation> violations = List.of(
                violationWithPoints(2)
        );
        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(violations);
        when(penaltyProperties.getThresholds()).thenReturn(List.of(
                threshold(5, PenaltyAction.WARNING, 7),
                threshold(10, PenaltyAction.BAN_TEMP, 30)
        ));

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        verify(penaltyRepository, never()).save(any());
        verify(violationRepository, never()).saveAll(any());
    }

    // ─── WARNING threshold met ────────────────────────────────────────────────

    @Test
    void should_create_WARNING_penalty_when_points_reach_warning_threshold() {
        List<BookingViolation> violations = List.of(
                violationWithPoints(3), violationWithPoints(3)
        );
        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(violations);
        when(penaltyProperties.getThresholds()).thenReturn(List.of(
                threshold(5, PenaltyAction.WARNING, 7),
                threshold(10, PenaltyAction.BAN_TEMP, 30)
        ));
        when(penaltyRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of());
        when(penaltyReasonUtils.createInitialReason(any(), anyInt(), anyInt())).thenReturn("reason_json");
        when(penaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        ArgumentCaptor<PenaltyRecord> captor = ArgumentCaptor.forClass(PenaltyRecord.class);
        verify(penaltyRepository).save(captor.capture());
        assertThat(captor.getValue().getPenaltyAction()).isEqualTo(PenaltyAction.WARNING);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    // ─── highest threshold wins ───────────────────────────────────────────────

    @Test
    void should_apply_BAN_TEMP_when_points_exceed_both_thresholds() {
        List<BookingViolation> violations = List.of(
                violationWithPoints(5), violationWithPoints(6)
        );
        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(violations);
        when(penaltyProperties.getThresholds()).thenReturn(List.of(
                threshold(5, PenaltyAction.WARNING, 7),
                threshold(10, PenaltyAction.BAN_TEMP, 30)
        ));
        when(penaltyRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of());
        when(penaltyReasonUtils.createInitialReason(any(), anyInt(), anyInt())).thenReturn("reason_json");
        when(penaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        ArgumentCaptor<PenaltyRecord> captor = ArgumentCaptor.forClass(PenaltyRecord.class);
        verify(penaltyRepository).save(captor.capture());
        assertThat(captor.getValue().getPenaltyAction()).isEqualTo(PenaltyAction.BAN_TEMP);
    }

    // ─── duplicate guard ──────────────────────────────────────────────────────

    @Test
    void should_not_create_new_penalty_when_same_type_already_active() {
        List<BookingViolation> violations = List.of(violationWithPoints(6));
        PenaltyRecord existing = PenaltyRecord.builder()
                .id(99L).user(user).penaltyAction(PenaltyAction.WARNING).isActive(true).build();

        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(violations);
        when(penaltyProperties.getThresholds()).thenReturn(List.of(
                threshold(5, PenaltyAction.WARNING, 7)
        ));
        when(penaltyRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(existing));

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        verify(penaltyRepository, never()).save(any());
    }

    @Test
    void should_link_violations_to_existing_penalty_when_duplicate_detected() {
        BookingViolation violation = violationWithPoints(6);
        List<BookingViolation> violations = List.of(violation);
        PenaltyRecord existing = PenaltyRecord.builder()
                .id(99L).user(user).penaltyAction(PenaltyAction.WARNING).isActive(true).build();

        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(violations);
        when(penaltyProperties.getThresholds()).thenReturn(List.of(
                threshold(5, PenaltyAction.WARNING, 7)
        ));
        when(penaltyRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(existing));

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        verify(violationRepository).saveAll(violations);
        assertThat(violation.getPenalty()).isEqualTo(existing);
    }

    // ─── violation linking on new penalty ────────────────────────────────────

    @Test
    void should_link_all_contributing_violations_to_new_penalty() {
        BookingViolation v1 = violationWithPoints(3);
        BookingViolation v2 = violationWithPoints(3);
        List<BookingViolation> violations = List.of(v1, v2);

        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(violations);
        when(penaltyProperties.getThresholds()).thenReturn(List.of(
                threshold(5, PenaltyAction.WARNING, 7)
        ));
        when(penaltyRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of());
        when(penaltyReasonUtils.createInitialReason(any(), anyInt(), anyInt())).thenReturn("reason_json");
        when(penaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        verify(violationRepository).saveAll(violations);
        assertThat(v1.getPenalty()).isNotNull();
        assertThat(v2.getPenalty()).isNotNull();
    }

    // ─── window query ─────────────────────────────────────────────────────────

    @Test
    void should_query_violations_within_configured_window_days() {
        when(penaltyProperties.getWindowDays()).thenReturn(14);
        when(violationRepository.findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any()))
                .thenReturn(List.of());
        when(penaltyProperties.getThresholds()).thenReturn(List.of());

        listener.handleViolationCreated(new ViolationCreatedEvent(10L, user.getId()));

        verify(violationRepository).findByUserIdAndPenaltyIsNullAndCreatedAtAfter(eq(user.getId()), any());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BookingViolation violationWithPoints(int points) {
        return BookingViolation.builder()
                .severityPoints(points)
                .reason("violation reason")
                .build();
    }

    private PenaltyProperties.Threshold threshold(int minPoints, PenaltyAction action, int durationDays) {
        PenaltyProperties.Threshold t = new PenaltyProperties.Threshold();
        t.setMinPoints(minPoints);
        t.setAction(action);
        t.setDurationDays(durationDays);
        return t;
    }
}
