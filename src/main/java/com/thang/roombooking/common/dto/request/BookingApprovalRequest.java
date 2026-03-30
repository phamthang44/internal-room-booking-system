package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.enums.ApprovalAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingApprovalRequest(
        @NotNull(message = "{validation.booking.id.required}")
        @Schema(description = "Booking id", example = "1")
        Long bookingId,

        @NotNull(message = "{validation.booking.action.required}")
        @Schema(description = "Hành động: APPROVE hoặc REJECT", example = "REJECT")
        ApprovalAction action,

        @Size(max = 500, message = "{validation.booking.reason.too_long}")
        @Schema(description = "Lý do (Bắt buộc nếu là REJECT)", example = "Phòng đang bảo trì đột xuất")
        String reason
) {
}
