package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.RoomAction;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.ClassroomErrorCode;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.policy.RoomPolicy;
import com.thang.roombooking.service.policy.context.RoomContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.thang.roombooking.common.constant.SystemConstant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteRoomPolicyImpl implements RoomPolicy {

    private final BookingRepository bookingRepository;

    @Override
    public RoomAction getAction() {
        return RoomAction.DELETE;
    }

    @Override
    public void validate(RoomContext context) {
        boolean hasFutureBooking = bookingRepository.hasUpcomingBookings(
                context.getRoomId(),
                List.of(com.thang.roombooking.common.enums.BookingStatus.PENDING, com.thang.roombooking.common.enums.BookingStatus.APPROVED),
                LocalDate.now(ZoneId.of(SystemConstant.SYSTEM_REGION_TIMEZONE)),
                LocalTime.now(ZoneId.of(SystemConstant.SYSTEM_REGION_TIMEZONE))
        );

        if (hasFutureBooking) {
            throw new AppException(ClassroomErrorCode.CANNOT_DELETE_ROOM_WITH_ACTIVE_BOOKINGS);
        }
    }
}