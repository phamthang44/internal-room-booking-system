package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingApproval;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.BookingApprovalRepository;
import com.thang.roombooking.service.BookingApprovalCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingApprovalCommandServiceImpl implements BookingApprovalCommandService {

    private final BookingApprovalRepository bookingApprovalRepository;

    @Override
    @Transactional
    public void saveApprovalBooking(Booking booking, UserAccount userAccount) {
        log.info("{} | Save approval booking | Data: {}", LogConstant.ACTION_START, booking.getId());
        try {
            BookingApproval bookingApproval = BookingApproval.builder()
                    .booking(booking)
                    .approvalStatus(booking.getStatus().name())
                    .approver(userAccount)
                    .note(booking.getRejectionReason())
                    .build();
            bookingApprovalRepository.save(bookingApproval);
            log.info("{} | Save approval booking success", LogConstant.ACTION_SUCCESS);
        } catch (Exception e) {
            log.error("{} | Unexpected System Error | ", LogConstant.SYS_ERROR, e);
        }
    }
}
