package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingHistory;
import com.thang.roombooking.repository.BookingHistoryRepository;
import com.thang.roombooking.service.BookingHistoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingHistoryCommandServiceImpl implements BookingHistoryCommandService {

    private final BookingHistoryRepository bookingHistoryRepository;

    @Override
    public void saveBookingHistory(Booking booking, String action, String performedBy, String note, BookingStatus bookingStatus) {
        BookingHistory history = BookingHistory.builder()
                .booking(booking)
                .action(action)
                .note(note)
                .performedBy(performedBy)
                .statusAfter(bookingStatus)
                .build();
        bookingHistoryRepository.save(history);
    }
}
