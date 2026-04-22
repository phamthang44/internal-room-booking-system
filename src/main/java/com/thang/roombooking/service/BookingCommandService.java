package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.*;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.UserAccount;

public interface BookingCommandService {

    CreateBookingResponse createBooking(CreateBookingRequest request, UserAccount currentUser);
    void checkIn(CheckInRequest request, UserAccount currentUser);
    void checkout(CheckoutRequest request, UserAccount currentUser);

    Long approveBooking(BookingApprovalRequest request, UserAccount currentUser);

    void rejectBooking(BookingApprovalRequest request, UserAccount currentUser);

    void cancelExpiredBooking(Booking booking);
    
    void autoRejectOverduePendingBooking(Booking booking);
    void autoCheckoutExpiredBooking(Booking booking);

    void cancelBooking(CancelBookingRequest req, UserAccount userAccount);
}
