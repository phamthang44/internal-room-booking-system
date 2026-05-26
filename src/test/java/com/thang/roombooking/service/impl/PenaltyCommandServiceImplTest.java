package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.PenaltyExtendRequest;
import com.thang.roombooking.common.dto.request.PenaltyRevokeRequest;
import com.thang.roombooking.common.enums.PenaltyAction;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.PenaltyErrorCode;
import com.thang.roombooking.common.utils.PenaltyReasonUtils;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.fixture.UserFixtures;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyCommandServiceImplTest {

    @Mock private PenaltyRecordRepository penaltyRecordRepository;
    @Mock private BookingViolationRepository violationRepository;
    @Mock private PenaltyReasonUtils penaltyReasonUtils;

    @InjectMocks
    private PenaltyCommandServiceImpl service;

    private final UserAccount admin = UserFixtures.adminUser();

    // ─── revokePenalty ─────────────────────────────────────────────────────────

    @Test
    void should_throw_PENALTY_NOT_FOUND_when_revoke_with_missing_id() {
        when(penaltyRecordRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.revokePenalty(99L, new PenaltyRevokeRequest("pardon"), admin));

        assertThat(ex.getErrorCode()).isEqualTo(PenaltyErrorCode.PENALTY_NOT_FOUND);
    }

    @Test
    void should_throw_ALREADY_INACTIVE_when_revoking_inactive_penalty() {
        PenaltyRecord inactive = activePenalty();
        inactive.setActive(false);
        when(penaltyRecordRepository.findById(1L)).thenReturn(Optional.of(inactive));

        AppException ex = assertThrows(AppException.class,
                () -> service.revokePenalty(1L, new PenaltyRevokeRequest("pardon"), admin));

        assertThat(ex.getErrorCode()).isEqualTo(PenaltyErrorCode.ALREADY_INACTIVE);
    }

    @Test
    void should_soft_delete_linked_violations_when_revoking_active_penalty() {
        PenaltyRecord penalty = activePenalty();
        List<BookingViolation> violations = List.of(
                BookingViolation.builder().id(10L).build(),
                BookingViolation.builder().id(11L).build()
        );

        when(penaltyRecordRepository.findById(1L)).thenReturn(Optional.of(penalty));
        when(violationRepository.findByPenalty(penalty)).thenReturn(violations);
        when(penaltyReasonUtils.appendReason(any(), any(), any())).thenReturn("updated reason");
        when(penaltyReasonUtils.translate(any())).thenReturn("translated reason");
        when(penaltyRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revokePenalty(1L, new PenaltyRevokeRequest("good behavior"), admin);

        verify(violationRepository).deleteAll(violations);
        assertThat(penalty.isActive()).isFalse();
        assertThat(penalty.getPenaltyAction()).isEqualTo(PenaltyAction.REVOKED);
    }

    @Test
    void should_not_call_deleteAll_when_no_linked_violations_on_revoke() {
        PenaltyRecord penalty = activePenalty();

        when(penaltyRecordRepository.findById(1L)).thenReturn(Optional.of(penalty));
        when(violationRepository.findByPenalty(penalty)).thenReturn(List.of());
        when(penaltyReasonUtils.appendReason(any(), any(), any())).thenReturn("reason");
        when(penaltyReasonUtils.translate(any())).thenReturn("translated");
        when(penaltyRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revokePenalty(1L, new PenaltyRevokeRequest("no violations"), admin);

        verify(violationRepository, never()).deleteAll(any());
    }

    // ─── extendPenalty ─────────────────────────────────────────────────────────

    @Test
    void should_throw_PENALTY_NOT_FOUND_when_extend_with_missing_id() {
        when(penaltyRecordRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.extendPenalty(99L, extendRequest(Instant.now().plusSeconds(3600)), admin));

        assertThat(ex.getErrorCode()).isEqualTo(PenaltyErrorCode.PENALTY_NOT_FOUND);
    }

    @Test
    void should_throw_ALREADY_INACTIVE_when_extending_inactive_penalty() {
        PenaltyRecord inactive = activePenalty();
        inactive.setActive(false);
        when(penaltyRecordRepository.findById(1L)).thenReturn(Optional.of(inactive));

        AppException ex = assertThrows(AppException.class,
                () -> service.extendPenalty(1L, extendRequest(Instant.now().plusSeconds(3600)), admin));

        assertThat(ex.getErrorCode()).isEqualTo(PenaltyErrorCode.ALREADY_INACTIVE);
    }

    @Test
    void should_throw_INVALID_EXTEND_DATE_when_new_date_is_before_current_end() {
        PenaltyRecord penalty = activePenalty();
        Instant currentEnd = Instant.now().plusSeconds(7200);
        penalty.setEndDate(currentEnd);
        when(penaltyRecordRepository.findById(1L)).thenReturn(Optional.of(penalty));

        Instant newEndBeforeCurrent = currentEnd.minusSeconds(1);
        AppException ex = assertThrows(AppException.class,
                () -> service.extendPenalty(1L, extendRequest(newEndBeforeCurrent), admin));

        assertThat(ex.getErrorCode()).isEqualTo(PenaltyErrorCode.INVALID_EXTEND_DATE);
    }

    @Test
    void should_update_end_date_when_new_date_is_after_current_end() {
        PenaltyRecord penalty = activePenalty();
        Instant currentEnd = Instant.now().plusSeconds(3600);
        penalty.setEndDate(currentEnd);

        Instant newEnd = currentEnd.plusSeconds(86400);

        when(penaltyRecordRepository.findById(1L)).thenReturn(Optional.of(penalty));
        when(penaltyReasonUtils.appendReason(any(), any(), any())).thenReturn("reason");
        when(penaltyReasonUtils.translate(any())).thenReturn("translated");
        when(penaltyRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.extendPenalty(1L, extendRequest(newEnd), admin);

        assertThat(penalty.getEndDate()).isEqualTo(newEnd);
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private PenaltyRecord activePenalty() {
        return PenaltyRecord.builder()
                .id(1L)
                .user(UserFixtures.studentUser())
                .penaltyAction(PenaltyAction.WARNING)
                .isActive(true)
                .startDate(Instant.now().minusSeconds(3600))
                .endDate(Instant.now().plusSeconds(86400))
                .reason("accumulated_points")
                .build();
    }

    private PenaltyExtendRequest extendRequest(Instant newEndDate) {
        return new PenaltyExtendRequest("extend reason", newEndDate);
    }
}
