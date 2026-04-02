package com.thang.roombooking.service;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;

public interface BookingHistoryCommandService {

    void saveBookingHistory(Booking booking, String action, String performedBy, String note, BookingStatus bookingStatus);

}
