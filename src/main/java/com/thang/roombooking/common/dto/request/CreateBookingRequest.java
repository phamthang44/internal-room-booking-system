package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.validator.ValidTimeSlotSelection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateBookingRequest(

        @Schema(description = "Classroom ID", example = "1")
        @NotNull(message = "{validation.booking.classroom_id.required}")
        Long classroomId,

        @NotNull(message = "{validation.booking.date.required}")
        @FutureOrPresent(message = "{validation.booking.date.future}")
        LocalDate bookingDate,

        @NotNull(message = "{validation.booking.time_slot_ids.required}")
        @NotEmpty(message = "{validation.booking.time_slot_ids.at_least_1_time_slot}")
        @ValidTimeSlotSelection
        List<Integer> timeSlotIds,

        @NotNull(message = "{validation.booking.time_booking.required}")
        @Schema(description = "Time at the booking moment")
        Instant timeBooking,

        @NotNull(message = "{validation.booking.number_of_attendees.required}")
        @Schema(description = "Number of attendees", example = "1")
        @Positive(message = "{validation.booking.number_of_attendees.positive}")
        @Min(value = 1, message = "{validation.booking.number_of_attendees.min}")
        @Max(value = 200, message = "{validation.booking.number_of_attendees.max}")
        Integer attendees,

        @NotBlank(message = "{validation.booking.purpose.required}")
        @Size(min = 1, max = 1000, message = "{validation.booking.purpose.size}")
        String purpose) {

        @Override
        public Instant timeBooking() {
                return timeBooking != null ? timeBooking : Instant.now();
        }
}
