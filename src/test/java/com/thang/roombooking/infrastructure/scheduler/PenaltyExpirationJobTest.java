package com.thang.roombooking.infrastructure.scheduler;

import com.thang.roombooking.repository.PenaltyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyExpirationJobTest {

    @Mock private PenaltyRecordRepository penaltyRepository;

    @InjectMocks
    private PenaltyExpirationJob job;

    @Test
    void should_call_deactivateExpiredPenalties_with_current_instant() {
        when(penaltyRepository.deactivateExpiredPenalties(any(Instant.class))).thenReturn(3);

        job.cleanupExpiredPenalties();

        verify(penaltyRepository).deactivateExpiredPenalties(any(Instant.class));
    }

    @Test
    void should_complete_without_error_when_no_expired_penalties() {
        when(penaltyRepository.deactivateExpiredPenalties(any(Instant.class))).thenReturn(0);

        job.cleanupExpiredPenalties();

        verify(penaltyRepository).deactivateExpiredPenalties(any(Instant.class));
    }

    @Test
    void should_swallow_exception_and_not_propagate() {
        when(penaltyRepository.deactivateExpiredPenalties(any(Instant.class)))
                .thenThrow(new RuntimeException("DB unavailable"));

        job.cleanupExpiredPenalties();

        // Job catches all exceptions internally — should not throw
    }
}
