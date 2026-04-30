package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.BookingApprovalRequest;
import com.thang.roombooking.common.dto.request.CheckInRequest;
import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.enums.ApprovalAction;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.Building;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.TimeSlot;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.service.*;
import com.thang.roombooking.service.policy.BookingPolicyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCommandServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingValidatorService bookingValidatorService;
    @Mock private TimeSlotService timeSlotService;
    @Mock private BookingPolicyManager bookingPolicyManager;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private TranslationService translationService;
    @Mock private BookingMapper bookingMapper;
    @Mock private BookingApprovalCommandService bookingApprovalCommandService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserAccount admin;

    @InjectMocks
    private BookingCommandServiceImpl service;

    // ─── approveBooking ───────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_ALREADY_PROCESSED_when_atomicApprove_returns_zero() {
        // Arrange
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0)
                .build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.APPROVE, null);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(bookingRepository.atomicApprove(eq(1L), eq(BookingStatus.APPROVED), eq(0)))
                .thenReturn(0);

        // Act
        AppException ex = assertThrows(AppException.class,
                () -> service.approveBooking(1L, req, admin));

        // Assert
        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
    }

    @Test
    void should_throw_INVALID_REQUEST_when_approval_action_is_REJECT() {
        // Arrange — admin sends REJECT to the approve endpoint
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0)
                .build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.REJECT, "wrong endpoint");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(pending));

        // Act
        AppException ex = assertThrows(AppException.class,
                () -> service.approveBooking(1L, req, admin));

        // Assert
        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }

    // ─── rejectBooking ────────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_REJECTION_REASON_REQUIRED_when_reason_is_blank() {
        // Arrange
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0)
                .build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.REJECT, "   ");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(pending));

        // Act
        AppException ex = assertThrows(AppException.class,
                () -> service.rejectBooking(1L, req, admin));

        // Assert
        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_REJECTION_REASON_REQUIRED);
    }

    // ─── checkIn ─────────────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_ACCESS_DENIED_when_checkIn_user_is_not_owner() {
        // Arrange
        UserAccount owner = mock(UserAccount.class);
        when(owner.getId()).thenReturn(1L);
        when(admin.getId()).thenReturn(2L);

        Booking booking = Booking.builder()
                .id(1L).status(BookingStatus.APPROVED).user(owner).version(0)
                .build();
        CheckInRequest req = new CheckInRequest(Instant.now());

        when(bookingRepository.findByIdWithTimeSlots(1L)).thenReturn(Optional.of(booking));

        // Act
        AppException ex = assertThrows(AppException.class,
                () -> service.checkIn(1L, req, admin));

        // Assert
        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
    }

    // ─── createBooking ────────────────────────────────────────────────────────

    @Test
    void should_split_non_consecutive_slots_into_two_separate_bookings() {
        // Arrange — slots with id=1 and id=3 are not consecutive (1+1 != 3)
        TimeSlot slot1 = TimeSlot.builder().id(1).startTime(LocalTime.of(7, 0)).endTime(LocalTime.of(9, 0)).build();
        TimeSlot slot3 = TimeSlot.builder().id(3).startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(15, 0)).build();

        Building building = Building.builder().id(5L).build();
        Classroom classroom = Classroom.builder().id(10L).building(building).build();

        CreateBookingRequest req = new CreateBookingRequest(
                10L, LocalDate.of(2026, 5, 1), List.of(1, 3), Instant.now(), 20, "Study");

        when(timeSlotService.getTimeSlotsByIds(List.of(1, 3))).thenReturn(List.of(slot1, slot3));
        when(classroomRepository.getReferenceById(10L)).thenReturn(classroom);
        when(translationService.getAllTimeSlotTranslations()).thenReturn(Map.of());
        when(translationService.getTranslations(any())).thenReturn(Map.of());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toCreateBookingResponse(any(), any())).thenReturn(mock(CreateBookingResponse.class));
        when(admin.getEmail()).thenReturn("user@test.com");

        // Act
        List<CreateBookingResponse> result = service.createBooking(req, admin);

        // Assert — two non-consecutive slot groups → two bookings
        assertThat(result).hasSize(2);
    }
}
