package com.thang.roombooking.infrastructure.mail;

/**
 * Centralized template name constants to avoid duplicated literals across services.
 */
public final class MailTemplateNames {

    private MailTemplateNames() {}

    public static final String OTP_CODE = "mail/otp-code";
    public static final String WELCOME = "mail/welcome";
    public static final String RESET_PASSWORD_SUCCESS = "mail/reset-password-success";
    public static final String BOOKING_NOTIFICATION = "mail/booking-notification";
}

