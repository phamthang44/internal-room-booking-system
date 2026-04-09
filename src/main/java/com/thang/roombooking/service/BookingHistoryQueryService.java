package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.BookingHistorySummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingHistoryQueryService {

    List<BookingHistorySummaryResponse> getSummaryHistoryBooking(Long bookingId, LocalDate today);

}
