package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.CreateBookingRequest;
import com.thang.roombooking.common.dto.response.CreateBookingResponse;

public interface BookingCommandService {

    CreateBookingResponse createBooking(CreateBookingRequest request);


}
