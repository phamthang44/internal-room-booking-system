package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.*;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.UserAccount;

import java.util.List;

public interface BookingCommandService {

    List<CreateBookingResponse> createBooking(CreateBookingRequest request, UserAccount currentUser);
    void checkIn(Long id, CheckInRequest request, UserAccount currentUser);
    void checkout(Long id, CheckoutRequest request, UserAccount currentUser);

    Long approveBooking(Long id, BookingApprovalRequest request, UserAccount currentUser);

    void rejectBooking(Long id, BookingApprovalRequest request, UserAccount currentUser);

    void cancelExpiredBooking(Booking booking);
    
    void autoRejectOverduePendingBooking(Booking booking);
    void autoCheckoutExpiredBooking(Booking booking);

    void cancelBooking(Long id, CancelBookingRequest req, UserAccount userAccount);
}
