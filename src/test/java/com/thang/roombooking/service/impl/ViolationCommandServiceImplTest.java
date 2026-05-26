package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.CreateViolationRequest;
import com.thang.roombooking.common.enums.ViolationType;
import com.thang.roombooking.common.event.ViolationCreatedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.fixture.BookingFixtures;
import com.thang.roombooking.fixture.UserFixtures;
import com.thang.roombooking.infrastructure.configuration.PenaltyProperties;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViolationCommandServiceImplTest {

    @Mock private BookingViolationRepository violationRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PenaltyProperties penaltyProperties;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ViolationCommandServiceImpl service;

    private final UserAccount admin = UserFixtures.adminUser();

    // ─── createManualViolation ────────────────────────────────────────────────

    @Test
    void should_throw_USER_NOT_FOUND_when_target_user_does_not_exist() {
        CreateViolationRequest req = buildRequest(999L, null, ViolationType.NO_SHOW, null);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.createManualViolation(req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    void should_throw_BOOKING_NOT_FOUND_when_booking_id_provided_but_missing() {
        UserAccount target = UserFixtures.studentUser();
        CreateViolationRequest req = buildRequest(target.getId(), 55L, ViolationType.MISCONDUCT, null);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(bookingRepository.findById(55L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.createManualViolation(req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
    }

    @Test
    void should_use_default_severity_points_when_none_provided() {
        UserAccount target = UserFixtures.studentUser();
        CreateViolationRequest req = buildRequest(target.getId(), null, ViolationType.NO_SHOW, null);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(penaltyProperties.getPoints()).thenReturn(Map.of(ViolationType.NO_SHOW, 5));
        when(violationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createManualViolation(req, admin);

        ArgumentCaptor<BookingViolation> captor = ArgumentCaptor.forClass(BookingViolation.class);
        verify(violationRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverityPoints()).isEqualTo(5);
    }

    @Test
    void should_use_provided_severity_points_when_explicitly_set() {
        UserAccount target = UserFixtures.studentUser();
        CreateViolationRequest req = buildRequest(target.getId(), null, ViolationType.MISCONDUCT, 10);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(violationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createManualViolation(req, admin);

        ArgumentCaptor<BookingViolation> captor = ArgumentCaptor.forClass(BookingViolation.class);
        verify(violationRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverityPoints()).isEqualTo(10);
    }

    @Test
    void should_publish_ViolationCreatedEvent_on_success() {
        UserAccount target = UserFixtures.studentUser();
        CreateViolationRequest req = buildRequest(target.getId(), null, ViolationType.NO_SHOW, 3);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(violationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createManualViolation(req, admin);

        verify(eventPublisher).publishEvent(any(ViolationCreatedEvent.class));
    }

    @Test
    void should_link_booking_when_booking_id_is_provided() {
        UserAccount target = UserFixtures.studentUser();
        Booking booking = BookingFixtures.pendingBooking();
        CreateViolationRequest req = buildRequest(target.getId(), booking.getId(), ViolationType.DAMAGED_EQUIPMENT, 4);

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(violationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createManualViolation(req, admin);

        ArgumentCaptor<BookingViolation> captor = ArgumentCaptor.forClass(BookingViolation.class);
        verify(violationRepository).save(captor.capture());
        assertThat(captor.getValue().getBooking()).isEqualTo(booking);
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private CreateViolationRequest buildRequest(Long userId, Long bookingId, ViolationType type, Integer points) {
        return CreateViolationRequest.builder()
                .userId(userId)
                .bookingId(bookingId)
                .type(type)
                .reason("Manual violation reason")
                .severityPoints(points)
                .build();
    }
}
