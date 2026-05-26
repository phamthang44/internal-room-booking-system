package com.thang.roombooking.fixture;

import com.thang.roombooking.common.enums.AttendanceStatus;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingTimeSlot;
import com.thang.roombooking.entity.UserAccount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class BookingFixtures {

    private BookingFixtures() {}

    public static Booking pendingBooking() {
        return Booking.builder()
                .id(1L)
                .status(BookingStatus.PENDING)
                .user(UserFixtures.studentUser())
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now().plusDays(1))
                .attendees(20)
                .purpose("Study group")
                .version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
    }

    public static Booking approvedBooking() {
        return Booking.builder()
                .id(2L)
                .status(BookingStatus.APPROVED)
                .user(UserFixtures.studentUser())
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now().plusDays(1))
                .attendees(20)
                .purpose("Study group")
                .version(0)
                .bookingTimeSlots(timeSlotsFor(pendingBookingId()))
                .build();
    }

    public static Booking approvedBookingFor(UserAccount user) {
        Booking b = approvedBooking();
        b.setUser(user);
        return b;
    }

    public static Booking checkedInBooking() {
        return Booking.builder()
                .id(3L)
                .status(BookingStatus.CHECKED_IN)
                .user(UserFixtures.studentUser())
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now())
                .attendees(20)
                .purpose("Study group")
                .version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
    }

    public static Booking cancelledBooking() {
        return Booking.builder()
                .id(4L)
                .status(BookingStatus.CANCELLED)
                .attendanceStatus(AttendanceStatus.NO_SHOW)
                .user(UserFixtures.studentUser())
                .classroom(ClassroomFixtures.available())
                .bookingDate(LocalDate.now())
                .attendees(20)
                .purpose("Study group")
                .version(0)
                .bookingTimeSlots(new ArrayList<>())
                .build();
    }

    private static Long pendingBookingId() {
        return 1L;
    }

    private static List<BookingTimeSlot> timeSlotsFor(Long bookingId) {
        BookingTimeSlot bts = BookingTimeSlot.builder()
                .timeSlot(TimeSlotFixtures.slot1())
                .build();
        return new ArrayList<>(List.of(bts));
    }
}
