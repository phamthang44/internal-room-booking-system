package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ApprovalSearchRequest {

    @Schema(description = "Hội viên hoặc mã phòng", example = "Student A")
    private String keyword;

    @Schema(description = "Trạng thái phê duyệt (APPROVED, REJECTED)", example = "APPROVED")
    private BookingStatus status;

    @Schema(description = "Từ ngày phê duyệt", example = "2024-03-01")
    private LocalDate fromDate;

    @Schema(description = "Đến ngày phê duyệt", example = "2024-03-31")
    private LocalDate toDate;

    @Schema(description = "Số trang (1-indexed)", example = "1")
    private int page = 1;

    @Schema(description = "Kích thước trang", example = "10")
    private int size = 10;
}
