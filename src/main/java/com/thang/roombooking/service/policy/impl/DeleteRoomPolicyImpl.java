package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.RoomAction;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.ClassroomErrorCode;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.policy.RoomPolicy;
import com.thang.roombooking.service.policy.context.RoomContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
        boolean hasFutureBooking = bookingRepository.existsByClassroomIdAndEndTimeAfter(
                context.getRoomId(),
                Instant.now()
        );

        if (hasFutureBooking) {
            throw new AppException(ClassroomErrorCode.CANNOT_DELETE_ROOM_WITH_ACTIVE_BOOKINGS);
        }
    }
}