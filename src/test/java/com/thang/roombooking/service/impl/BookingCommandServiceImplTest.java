package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.*;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.common.enums.ApprovalAction;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.ClassroomErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.*;
import com.thang.roombooking.fixture.BookingFixtures;
import com.thang.roombooking.fixture.ClassroomFixtures;
import com.thang.roombooking.fixture.TimeSlotFixtures;
import com.thang.roombooking.fixture.UserFixtures;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0)
                .build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.APPROVE, null);

        when(bookingRepository.findByIdWithLock(1L)).thenReturn(Optional.of(pending));
        when(bookingRepository.atomicApprove(eq(1L), eq(BookingStatus.APPROVED), eq(0)))
                .thenReturn(0);

        AppException ex = assertThrows(AppException.class,
                () -> service.approveBooking(1L, req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
    }

    @Test
    void should_throw_INVALID_REQUEST_when_approval_action_is_REJECT() {
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0)
                .build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.REJECT, "wrong endpoint");

        when(bookingRepository.findByIdWithLock(1L)).thenReturn(Optional.of(pending));

        AppException ex = assertThrows(AppException.class,
                () -> service.approveBooking(1L, req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }

    @Test
    void should_return_booking_id_and_publish_event_when_approve_succeeds() {
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0).build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.APPROVE, null);
        Booking approved = Booking.builder()
                .id(1L).status(BookingStatus.APPROVED).version(1).build();

        when(bookingRepository.findByIdWithLock(1L)).thenReturn(Optional.of(pending));
        when(bookingRepository.atomicApprove(1L, BookingStatus.APPROVED, 0)).thenReturn(1);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(approved));
        when(admin.getEmail()).thenReturn("admin@uni.edu.vn");

        Long result = service.approveBooking(1L, req, admin);

        assertThat(result).isEqualTo(1L);
        verify(bookingApprovalCommandService).saveApprovalBooking(approved, admin);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    // ─── rejectBooking ────────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_REJECTION_REASON_REQUIRED_when_reason_is_blank() {
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0)
                .build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.REJECT, "   ");

        when(bookingRepository.findByIdWithLock(1L)).thenReturn(Optional.of(pending));

        AppException ex = assertThrows(AppException.class,
                () -> service.rejectBooking(1L, req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_REJECTION_REASON_REQUIRED);
    }

    @Test
    void should_publish_event_and_save_approval_when_reject_succeeds() {
        Booking pending = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).version(0).build();
        BookingApprovalRequest req = new BookingApprovalRequest(ApprovalAction.REJECT, "Room already reserved");
        Booking rejected = Booking.builder()
                .id(1L).status(BookingStatus.REJECTED).version(1).build();

        when(bookingRepository.findByIdWithLock(1L)).thenReturn(Optional.of(pending));
        when(bookingRepository.atomicRejectPending(eq(1L), eq(BookingStatus.REJECTED), any(), eq(0))).thenReturn(1);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(rejected));
        when(admin.getEmail()).thenReturn("admin@uni.edu.vn");

        service.rejectBooking(1L, req, admin);

        verify(bookingApprovalCommandService).saveApprovalBooking(rejected, admin);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    // ─── checkIn ─────────────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_ACCESS_DENIED_when_checkIn_user_is_not_owner() {
        UserAccount owner = mock(UserAccount.class);
        when(owner.getId()).thenReturn(1L);
        when(admin.getId()).thenReturn(2L);

        Booking booking = Booking.builder()
                .id(1L).status(BookingStatus.APPROVED).user(owner).version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
        CheckInRequest req = new CheckInRequest(Instant.now());

        when(bookingRepository.findByIdWithTimeSlotsAndLock(1L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class,
                () -> service.checkIn(1L, req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
    }

    @Test
    void should_throw_BOOKING_NOT_FOUND_when_checkIn_booking_missing() {
        when(bookingRepository.findByIdWithTimeSlotsAndLock(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.checkIn(1L, new CheckInRequest(Instant.now()), admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
    }

    @Test
    void should_publish_event_when_checkIn_succeeds() {
        UserAccount student = UserFixtures.studentUser();
        Booking booking = approvedBookingWithStartTime(student);
        Booking updated = approvedBookingWithStartTime(student);

        when(admin.getId()).thenReturn(student.getId());
        when(admin.getEmail()).thenReturn(student.getEmail());
        when(bookingRepository.findByIdWithTimeSlotsAndLock(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.atomicCheckIn(eq(booking.getId()), any(), eq(0))).thenReturn(1);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(updated));

        service.checkIn(booking.getId(), new CheckInRequest(Instant.now()), admin);

        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void should_throw_BOOKING_ALREADY_CHECKED_IN_when_atomicCheckIn_returns_zero_and_status_is_CHECKED_IN() {
        UserAccount student = UserFixtures.studentUser();
        Booking booking = approvedBookingWithStartTime(student);
        Booking alreadyCheckedIn = Booking.builder()
                .id(booking.getId()).status(BookingStatus.CHECKED_IN).version(1)
                .bookingTimeSlots(new ArrayList<>()).build();

        when(admin.getId()).thenReturn(student.getId());
        when(bookingRepository.findByIdWithTimeSlotsAndLock(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.atomicCheckIn(eq(booking.getId()), any(), eq(0))).thenReturn(0);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(alreadyCheckedIn));

        AppException ex = assertThrows(AppException.class,
                () -> service.checkIn(booking.getId(), new CheckInRequest(Instant.now()), admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ALREADY_CHECKED_IN);
    }

    // ─── checkout ─────────────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_ACCESS_DENIED_when_checkout_user_is_not_owner() {
        UserAccount owner = mock(UserAccount.class);
        when(owner.getId()).thenReturn(1L);
        when(admin.getId()).thenReturn(2L);

        Booking booking = Booking.builder()
                .id(3L).status(BookingStatus.CHECKED_IN).user(owner).version(0)
                .bookingTimeSlots(new ArrayList<>()).build();
        CheckoutRequest req = new CheckoutRequest(Instant.now(), 15);

        when(bookingRepository.findByIdWithTimeSlotsAndLock(3L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class,
                () -> service.checkout(3L, req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
    }

    @Test
    void should_throw_BOOKING_NOT_CHECKED_IN_when_status_is_not_CHECKED_IN() {
        UserAccount student = UserFixtures.studentUser();
        Booking booking = Booking.builder()
                .id(3L).status(BookingStatus.APPROVED).user(student).version(0)
                .bookingTimeSlots(new ArrayList<>()).build();

        when(admin.getId()).thenReturn(student.getId());
        when(bookingRepository.findByIdWithTimeSlotsAndLock(3L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class,
                () -> service.checkout(3L, new CheckoutRequest(Instant.now(), 10), admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_NOT_CHECKED_IN);
    }

    @Test
    void should_publish_event_when_checkout_succeeds() {
        UserAccount student = UserFixtures.studentUser();
        Booking booking = checkedInBookingWithStartTime(student);
        Booking completed = Booking.builder()
                .id(booking.getId()).status(BookingStatus.COMPLETED).version(1)
                .bookingTimeSlots(new ArrayList<>()).build();

        when(admin.getId()).thenReturn(student.getId());
        when(admin.getEmail()).thenReturn(student.getEmail());
        when(bookingRepository.findByIdWithTimeSlotsAndLock(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.atomicCheckoutToCompleted(eq(booking.getId()), any(), any(), eq(0))).thenReturn(1);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(completed));

        service.checkout(booking.getId(), new CheckoutRequest(Instant.now(), 15), admin);

        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    // ─── cancelBooking ────────────────────────────────────────────────────────

    @Test
    void should_throw_BOOKING_ACCESS_DENIED_when_cancel_user_is_not_owner() {
        UserAccount owner = mock(UserAccount.class);
        when(owner.getId()).thenReturn(1L);
        when(admin.getId()).thenReturn(2L);

        Booking booking = Booking.builder()
                .id(1L).status(BookingStatus.PENDING).user(owner).version(0)
                .bookingTimeSlots(new ArrayList<>()).build();

        when(bookingRepository.findByIdWithTimeSlotsAndLock(1L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class,
                () -> service.cancelBooking(1L, new CancelBookingRequest("no longer needed", Instant.now()), admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
    }

    @Test
    void should_throw_BOOKING_ALREADY_PROCESSED_when_atomicCancelByStudent_returns_zero() {
        UserAccount student = UserFixtures.studentUser();
        Booking booking = bookingWithStartTime(student, BookingStatus.PENDING);

        when(admin.getId()).thenReturn(student.getId());
        when(admin.getUsername()).thenReturn(student.getUsername());
        when(bookingRepository.findByIdWithTimeSlotsAndLock(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.atomicCancelByStudent(any(), any(), any(), any(), any())).thenReturn(0);

        AppException ex = assertThrows(AppException.class,
                () -> service.cancelBooking(booking.getId(), new CancelBookingRequest("changed mind", Instant.now()), admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
    }

    @Test
    void should_publish_event_and_check_spam_when_cancelBooking_succeeds() {
        UserAccount student = UserFixtures.studentUser();
        Booking booking = bookingWithStartTime(student, BookingStatus.APPROVED);
        Booking cancelled = BookingFixtures.cancelledBooking();
        cancelled.setUser(student);

        when(admin.getId()).thenReturn(student.getId());
        when(admin.getUsername()).thenReturn(student.getUsername());
        when(bookingRepository.findByIdWithTimeSlotsAndLock(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.atomicCancelByStudent(any(), any(), any(), any(), any())).thenReturn(1);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(cancelled));

        service.cancelBooking(booking.getId(), new CancelBookingRequest("changed plan", Instant.now()), admin);

        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
        verify(bookingPolicyManager).checkCancellationSpam(cancelled);
    }

    // ─── cancelExpiredBooking ─────────────────────────────────────────────────

    @Test
    void should_handle_no_show_and_publish_event_when_atomicCancel_returns_nonzero() {
        Booking booking = BookingFixtures.approvedBooking();

        when(bookingRepository.atomicCancel(eq(booking.getId()), eq(BookingStatus.CANCELLED), any(), eq(0)))
                .thenReturn(1);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.cancelExpiredBooking(booking);

        verify(bookingPolicyManager).handleNoShowViolation(booking);
        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void should_do_nothing_when_atomicCancel_returns_zero_in_cancelExpiredBooking() {
        Booking booking = BookingFixtures.approvedBooking();

        when(bookingRepository.atomicCancel(eq(booking.getId()), any(), any(), eq(0))).thenReturn(0);

        service.cancelExpiredBooking(booking);

        verify(bookingPolicyManager, never()).handleNoShowViolation(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ─── autoRejectOverduePendingBooking ───────────────────────────────────────

    @Test
    void should_publish_event_when_autoRejectOverduePendingBooking_updates_row() {
        Booking booking = BookingFixtures.pendingBooking();

        when(bookingRepository.atomicRejectPendingBySystem(
                eq(booking.getId()), eq(BookingStatus.REJECTED), any(), any(), eq(0))).thenReturn(1);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.autoRejectOverduePendingBooking(booking);

        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    @Test
    void should_do_nothing_when_autoRejectOverduePendingBooking_updates_zero_rows() {
        Booking booking = BookingFixtures.pendingBooking();

        when(bookingRepository.atomicRejectPendingBySystem(
                eq(booking.getId()), any(), any(), any(), eq(0))).thenReturn(0);

        service.autoRejectOverduePendingBooking(booking);

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ─── autoCheckoutExpiredBooking ────────────────────────────────────────────

    @Test
    void should_publish_event_when_autoCheckout_succeeds() {
        Booking booking = BookingFixtures.checkedInBooking();

        when(bookingRepository.atomicAutoCheckoutToCompleted(
                eq(booking.getId()), any(), any(), eq(0))).thenReturn(1);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.autoCheckoutExpiredBooking(booking);

        verify(eventPublisher).publishEvent(any(BookingStatusChangedEvent.class));
    }

    // ─── createBooking ────────────────────────────────────────────────────────

    @Test
    void should_split_non_consecutive_slots_into_two_separate_bookings() {
        TimeSlot slot1 = TimeSlotFixtures.slot1();
        TimeSlot slot3 = TimeSlotFixtures.slot3();

        Building building = ClassroomFixtures.building();
        Classroom classroom = ClassroomFixtures.available();

        CreateBookingRequest req = new CreateBookingRequest(
                classroom.getId(), LocalDate.of(2026, 5, 1), List.of(slot1.getId(), slot3.getId()),
                Instant.now(), 20, "Study");

        when(classroomRepository.findByIdWithLock(classroom.getId())).thenReturn(Optional.of(classroom));
        when(timeSlotService.getTimeSlotsByIds(List.of(slot1.getId(), slot3.getId())))
                .thenReturn(List.of(slot1, slot3));
        when(translationService.getAllTimeSlotTranslations()).thenReturn(Map.of());
        when(translationService.getTranslations(any())).thenReturn(Map.of());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toCreateBookingResponse(any(), any())).thenReturn(mock(CreateBookingResponse.class));
        when(admin.getEmail()).thenReturn("user@test.com");

        List<CreateBookingResponse> result = service.createBooking(req, admin);

        assertThat(result).hasSize(2);
    }

    @Test
    void should_create_single_booking_for_consecutive_slots() {
        TimeSlot slot1 = TimeSlotFixtures.slot1();
        TimeSlot slot2 = TimeSlotFixtures.slot2();
        Classroom classroom = ClassroomFixtures.available();

        CreateBookingRequest req = new CreateBookingRequest(
                classroom.getId(), LocalDate.of(2026, 5, 1), List.of(slot1.getId(), slot2.getId()),
                Instant.now(), 20, "Study");

        when(classroomRepository.findByIdWithLock(classroom.getId())).thenReturn(Optional.of(classroom));
        when(timeSlotService.getTimeSlotsByIds(List.of(slot1.getId(), slot2.getId())))
                .thenReturn(List.of(slot1, slot2));
        when(translationService.getAllTimeSlotTranslations()).thenReturn(Map.of());
        when(translationService.getTranslations(any())).thenReturn(Map.of());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toCreateBookingResponse(any(), any())).thenReturn(mock(CreateBookingResponse.class));
        when(admin.getEmail()).thenReturn("user@test.com");

        List<CreateBookingResponse> result = service.createBooking(req, admin);

        assertThat(result).hasSize(1);
    }

    @Test
    void should_throw_CLASSROOM_NOT_FOUND_when_classroom_does_not_exist() {
        CreateBookingRequest req = new CreateBookingRequest(
                999L, LocalDate.now().plusDays(1), List.of(1), Instant.now(), 10, "Study");

        when(classroomRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.createBooking(req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(ClassroomErrorCode.CLASSROOM_NOT_FOUND);
    }

    @Test
    void should_throw_BOOKING_USER_RESTRICTED_when_user_has_active_penalty() {
        Classroom classroom = ClassroomFixtures.available();
        CreateBookingRequest req = new CreateBookingRequest(
                classroom.getId(), LocalDate.now().plusDays(1), List.of(1), Instant.now(), 10, "Study");

        when(classroomRepository.findByIdWithLock(classroom.getId())).thenReturn(Optional.of(classroom));
        doThrow(new AppException(BookingErrorCode.BOOKING_USER_RESTRICTED))
                .when(bookingPolicyManager).validatePenalty(any());

        AppException ex = assertThrows(AppException.class,
                () -> service.createBooking(req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_USER_RESTRICTED);
    }

    @Test
    void should_throw_BOOKING_QUOTA_EXCEEDED_when_pending_quota_is_full() {
        Classroom classroom = ClassroomFixtures.available();
        CreateBookingRequest req = new CreateBookingRequest(
                classroom.getId(), LocalDate.now().plusDays(1), List.of(1), Instant.now(), 10, "Study");

        when(classroomRepository.findByIdWithLock(classroom.getId())).thenReturn(Optional.of(classroom));
        doThrow(new AppException(BookingErrorCode.BOOKING_QUOTA_EXCEEDED, 3, 1))
                .when(bookingPolicyManager).validatePendingQuota(any());

        AppException ex = assertThrows(AppException.class,
                () -> service.createBooking(req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_QUOTA_EXCEEDED);
    }

    @Test
    void should_throw_BOOKING_SLOT_ALREADY_TAKEN_when_room_has_conflict() {
        Classroom classroom = ClassroomFixtures.available();
        CreateBookingRequest req = new CreateBookingRequest(
                classroom.getId(), LocalDate.now().plusDays(1), List.of(1), Instant.now(), 10, "Study");

        when(classroomRepository.findByIdWithLock(classroom.getId())).thenReturn(Optional.of(classroom));
        doThrow(new AppException(BookingErrorCode.BOOKING_SLOT_ALREADY_TAKEN))
                .when(bookingPolicyManager).validateNoRoomConflict(any(), any(), any());

        AppException ex = assertThrows(AppException.class,
                () -> service.createBooking(req, admin));

        assertThat(ex.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_SLOT_ALREADY_TAKEN);
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private Booking approvedBookingWithStartTime(UserAccount user) {
        return Booking.builder()
                .id(2L).status(BookingStatus.APPROVED).user(user)
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now())
                .startTime(LocalTime.of(7, 0))
                .attendees(20).purpose("Study").version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
    }

    private Booking checkedInBookingWithStartTime(UserAccount user) {
        return Booking.builder()
                .id(3L).status(BookingStatus.CHECKED_IN).user(user)
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now())
                .startTime(LocalTime.of(7, 0))
                .attendees(20).purpose("Study").version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
    }

    private Booking bookingWithStartTime(UserAccount user, BookingStatus status) {
        return Booking.builder()
                .id(1L).status(status).user(user)
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now())
                .startTime(LocalTime.of(7, 0))
                .attendees(20).purpose("Study").version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
    }
}
