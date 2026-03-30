package com.thang.roombooking.service;

import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.UserAccount;

public interface BookingApprovalCommandService {

    void saveApprovalBooking(Booking booking, UserAccount userAccount);

}
