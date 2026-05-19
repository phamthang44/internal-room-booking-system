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

import com.thang.roombooking.common.constant.SystemConstant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeactivateBuildingPolicyImpl implements BuildingPolicy {

    private final BookingRepository bookingRepository;

    @Override
    public BuildingAction getAction() {
        return BuildingAction.DEACTIVATE;
    }

    @Override
    public void validate(BuildingContext context) {
        if (!context.isCurrentIsActive()) return;

        boolean hasUpcoming = bookingRepository.hasUpcomingBookingsForBuilding(
                context.getBuildingId(),
                List.of(BookingStatus.PENDING, BookingStatus.APPROVED),
                LocalDate.now(ZoneId.of(SystemConstant.SYSTEM_REGION_TIMEZONE)),
                LocalTime.now(ZoneId.of(SystemConstant.SYSTEM_REGION_TIMEZONE))
        );
        if (hasUpcoming) {
            throw new AppException(BuildingErrorCode.CANNOT_DEACTIVATE_BUILDING_WITH_UPCOMING_BOOKINGS);
        }
    }
}
