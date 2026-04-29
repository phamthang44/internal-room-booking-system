package com.thang.roombooking.service.policy.impl;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.BuildingAction;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.BuildingErrorCode;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.service.policy.BuildingPolicy;
import com.thang.roombooking.service.policy.context.BuildingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteBuildingPolicyImpl implements BuildingPolicy {

    private final BookingRepository bookingRepository;

    @Override
    public BuildingAction getAction() {
        return BuildingAction.DELETE;
    }

    @Override
    public void validate(BuildingContext context) {
        boolean hasActiveBookings = bookingRepository.hasUpcomingBookingsForBuilding(
                context.getBuildingId(),
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED, BookingStatus.CHECKED_IN),
                LocalDate.now(),
                LocalTime.now()
        );
        if (hasActiveBookings) {
            throw new AppException(BuildingErrorCode.CANNOT_DELETE_BUILDING_WITH_ACTIVE_CLASSROOMS);
        }
    }
}
