package com.thang.roombooking.common.constant;

public final class BookingMessageKeys {

    private BookingMessageKeys() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String HISTORY_PENDING = "booking.history.pending";
    public static final String HISTORY_CANCELLED = "booking.history.cancelled";
    public static final String HISTORY_APPROVED = "booking.history.approved";
    public static final String HISTORY_CHECKED_IN = "booking.history.checked_in";
    public static final String HISTORY_REJECTED_NO_REASON = "booking.history.rejected.no_reason";
    public static final String HISTORY_DEFAULT = "booking.history.default";
    public static final String HISTORY_NOTE_DEFAULT = "booking.history.note.default";
}
