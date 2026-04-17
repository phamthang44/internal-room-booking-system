package com.thang.roombooking.infrastructure.mail;

/**
 * i18n keys used by mail services (Sonar-friendly, avoids scattered string literals).
 */
public final class EmailI18nKeys {

    private EmailI18nKeys() {}

    public static final String ERROR_EMAIL_SERVICE = "error.email_service_error";

    public static final String GREETING_DEFAULT = "email.greeting.default";
    public static final String OTP_VERIFY_MESSAGE = "email.otp_verify.message";
    public static final String PASSWORD_RESET_OTP_MESSAGE = "email.password_reset_otp.message";

    public static final String WELCOME_SUBJECT = "email.welcome.subject";
    public static final String WELCOME_MESSAGE = "email.welcome.message";
    public static final String WELCOME_CTA = "email.welcome.cta";

    public static final String PASSWORD_RESET_SUCCESS_SUBJECT = "email.password_reset_success.subject";

    public static final String BOOKING_CREATED_PENDING_SUBJECT = "email.booking.created_pending.subject";
    public static final String BOOKING_CREATED_PENDING_TITLE = "email.booking.created_pending.title";
    public static final String BOOKING_CREATED_PENDING_SUBTITLE = "email.booking.created_pending.subtitle";
    public static final String BOOKING_CREATED_PENDING_MESSAGE = "email.booking.created_pending.message";

    public static final String BOOKING_STATUS_CHANGED_SUBJECT = "email.booking.status_changed.subject";
    public static final String BOOKING_STATUS_CHANGED_TITLE = "email.booking.status_changed.title";
    public static final String BOOKING_STATUS_CHANGED_SUBTITLE = "email.booking.status_changed.subtitle";
    public static final String BOOKING_STATUS_CHANGED_MESSAGE = "email.booking.status_changed.message";

    public static final String BOOKING_CONCURRENT_SUBJECT = "email.booking.concurrent.subject";
    public static final String BOOKING_CONCURRENT_TITLE = "email.booking.concurrent.title";
    public static final String BOOKING_CONCURRENT_SUBTITLE = "email.booking.concurrent.subtitle";
    public static final String BOOKING_CONCURRENT_MESSAGE = "email.booking.concurrent.message";
    public static final String BOOKING_CONCURRENT_ALERT = "email.booking.concurrent.alert";

    public static final String BOOKING_CANCELLED_SUBJECT = "email.booking.cancelled.subject";
    public static final String BOOKING_CANCELLED_TITLE = "email.booking.cancelled.title";
    public static final String BOOKING_CANCELLED_SUBTITLE = "email.booking.cancelled.subtitle";
    public static final String BOOKING_CANCELLED_MESSAGE = "email.booking.cancelled.message";
    public static final String BOOKING_CANCELLED_CANCELLED_BY = "email.booking.cancelled.cancelled_by";
}

