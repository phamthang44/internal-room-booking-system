package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.RoomAction;
import com.thang.roombooking.common.enums.RoomStatus;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.ClassroomErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.policy.RoomPolicy;
import com.thang.roombooking.service.policy.context.RoomContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChangeRoomStatusPolicyImpl implements RoomPolicy {

    private final BookingRepository bookingRepository;

    @Override
    public RoomAction getAction() {
        return RoomAction.CHANGE_STATUS;
    }

    @Override
    public void validate(RoomContext context) {
        validateStatusTransition(context.getCurrentStatus(), context.getNewStatus());

        switch (context.getNewStatus()) {
            case INACTIVE, DELETED, MAINTENANCE -> validateNoActiveBookings(context.getRoomId());
            case AVAILABLE -> { /* Logic cho active nếu cần */ }
        }
    }

    private void validateStatusTransition(RoomStatus current, RoomStatus target) {
        if (current == target) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Trạng thái mới phải khác trạng thái hiện tại");
        }
        if (current == RoomStatus.DELETED) {
            throw new AppException(ClassroomErrorCode.CANNOT_CHANGE_STATUS_OF_DELETED_ROOM);
        }
        if (!isValidTransition(current, target)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Chuyển đổi trạng thái không hợp lệ");
        }
    }

    private boolean isValidTransition(RoomStatus current, RoomStatus target) {
        return switch (current) {
            case AVAILABLE -> target == RoomStatus.INACTIVE || target == RoomStatus.MAINTENANCE;
            case INACTIVE -> target == RoomStatus.AVAILABLE || target == RoomStatus.MAINTENANCE;
            case MAINTENANCE -> target == RoomStatus.AVAILABLE || target == RoomStatus.INACTIVE;
            case DELETED -> false;
        };
    }

    private void validateNoActiveBookings(Long roomId) {
        boolean hasBookings = bookingRepository.existsByClassroomIdAndStatusInAndEndTimeAfter(
                roomId,
                List.of(BookingStatus.APPROVED, BookingStatus.PENDING),
                Instant.now()
        );
        if (hasBookings) {
            throw new AppException(ClassroomErrorCode.CANNOT_DEACTIVATE_ROOM_WITH_UPCOMING_BOOKINGS);
        }
    }
}