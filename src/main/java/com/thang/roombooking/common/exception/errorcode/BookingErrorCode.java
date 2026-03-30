package com.thang.roombooking.common.exception.errorcode;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BookingErrorCode implements BaseErrorCode {
    // Nhóm conflict & tranh chấp & kĩ thuật
    BOOKING_ALREADY_EXISTS(HttpStatus.CONFLICT, "BK_010", "error.booking.request_already_processed"), //user nhấn nút request gửi 2 lần
    BOOKING_SLOT_OVERLAP(HttpStatus.CONFLICT, "BK_011", "error.booking.slot_already_taken"), //slot này khung giờ này đã có ai đó nhanh chân hơn
    BOOKING_VERSION_CONFLICT(HttpStatus.CONFLICT, "BK_012", ""), //xảy ra khi 2 admin approve cùng 1 đơn


    // Nhóm Lead Time Policy (Quy định thời gian đặt)
    BOOKING_DATE_IN_PAST(HttpStatus.BAD_REQUEST, "BK_001", "error.booking.date_in_past"),
    BOOKING_DATE_OVER_LIMIT(HttpStatus.BAD_REQUEST, "BK_002", "error.booking.date_over_7_days"),
    BOOKING_OUT_OF_WORKING_HOURS(HttpStatus.BAD_REQUEST, "BK_003", "error.booking.outside_working_hours"),

    // Nhóm Quota & Penalty Policy (Hạn mức và Vi phạm)
    BOOKING_QUOTA_EXCEEDED(HttpStatus.BAD_REQUEST, "BK_004", "error.booking.quota_exceeded"),
    BOOKING_USER_RESTRICTED(HttpStatus.FORBIDDEN, "BK_005", "error.booking.user_is_blacklisted"),

    // Nhóm Check-in Policy (Luồng vận hành)
    BOOKING_CHECK_IN_TOO_EARLY(HttpStatus.BAD_REQUEST, "BK_006", "error.booking.check_in_too_early"),
    BOOKING_CHECK_IN_EXPIRED(HttpStatus.BAD_REQUEST, "BK_007", "error.booking.check_in_expired"),
    // Nhóm Check-in Policy (Lệch Ngày)
    BOOKING_CHECK_IN_TOO_EARLY_DAYS(HttpStatus.BAD_REQUEST, "BK_016", "error.booking.check_in_too_early_days"),
    BOOKING_CHECK_IN_EXPIRED_DAYS(HttpStatus.BAD_REQUEST, "BK_017", "error.booking.check_in_expired_days"),

    // Nhóm Check-in Policy (Lệch Phút trong cùng ngày)
    BOOKING_CHECK_IN_TOO_EARLY_MINUTES(HttpStatus.BAD_REQUEST, "BK_018", "error.booking.check_in_too_early_minutes"),
    BOOKING_CHECK_IN_EXPIRED_MINUTES(HttpStatus.BAD_REQUEST, "BK_019", "error.booking.check_in_expired_minutes"),


    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "BK_011", "error.booking.not_found"),
    BOOKING_NOT_APPROVED(HttpStatus.BAD_REQUEST, "BK_012", "error.booking.not_approved"),
    BOOKING_ALREADY_CHECKED_IN(HttpStatus.BAD_REQUEST, "BK_013", "error.booking.already_checked_in"),
    BOOKING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BK_014", "error.booking.access_denied"),
    BOOKING_CHECK_IN_WRONG_DATE(HttpStatus.BAD_REQUEST, "BK_015", "error.booking.check_in_wrong_date"),

    // Nhóm Integrity & Conflict (Tranh chấp dữ liệu)
    BOOKING_SLOT_ALREADY_TAKEN(HttpStatus.CONFLICT, "BK_008", "error.booking.slot_already_taken"),
    BOOKING_IDEMPOTENCY_REPLAY(HttpStatus.CONFLICT, "BK_009", "error.booking.request_already_processed"),

    TIMESLOT_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "BK_010", "error.booking.timeslot_already_ended"),

    BOOKING_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "BK_016", "error.booking.rejection_reason_required"),
    BOOKING_ALREADY_PROCESSED(HttpStatus.CONFLICT, "BK_017", "error.booking.already_processed_by_another_admin");
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageKey;

    @Override
    public String getMessage() {
        return messageKey;
    }

    public String format(Object... args) {
        try {
            if (args == null || args.length == 0) {
                return I18nUtils.get(messageKey);
            }
            return I18nUtils.get(messageKey, args);
        } catch (Exception e) {
            return messageKey;
        }
    }
}
