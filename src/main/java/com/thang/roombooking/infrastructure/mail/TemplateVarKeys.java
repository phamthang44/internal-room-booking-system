package com.thang.roombooking.infrastructure.mail;

/**
 * Centralized Thymeleaf variable keys used across mail templates.
 * Keeps map keys consistent and avoids duplicated string literals (Sonar-friendly).
 */
public final class TemplateVarKeys {

    private TemplateVarKeys() {}

    // Common app/recipient
    public static final String APP_NAME = "appName";
    public static final String USER_NAME = "userName";
    public static final String SUPPORT_EMAIL = "supportEmail";

    // Links
    public static final String HELP_CENTER_URL = "helpCenterUrl";
    public static final String PRIVACY_POLICY_URL = "privacyPolicyUrl";
    public static final String TERMS_URL = "termsUrl";
    public static final String UNSUBSCRIBE_URL = "unsubscribeUrl";
    public static final String LOGIN_URL = "loginUrl";
    public static final String SECURITY_URL = "securityUrl";

    // OTP template
    public static final String GREETING = "greeting";
    public static final String MESSAGE = "message";
    public static final String OTP_CODE = "otpCode";
    public static final String EXPIRY_MINUTES = "expiryMinutes";

    // Welcome template
    public static final String TAGLINE = "tagline";
    public static final String WELCOME_MESSAGE = "welcomeMessage";
    public static final String CTA_URL = "ctaUrl";
    public static final String CTA_TEXT = "ctaText";

    // Reset password success template
    public static final String USER_EMAIL = "userEmail";
    public static final String RESET_TIMESTAMP = "resetTimestamp";

    // Booking notification template
    public static final String HEADER_BG = "headerBg";
    public static final String HEADER_ICON_SVG = "headerIconSvg";
    public static final String TITLE = "title";
    public static final String SUBTITLE = "subtitle";
    public static final String INTRO = "intro";

    public static final String BOOKING_ID = "bookingId";
    public static final String ROOM_NAME = "roomName";
    public static final String BOOKING_DATE = "bookingDate";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String ATTENDEES = "attendees";
    public static final String PURPOSE = "purpose";

    public static final String STATUS_TEXT = "statusText";
    public static final String STATUS_BG = "statusBg";
    public static final String STATUS_COLOR = "statusColor";
    public static final String STATUS_DOT = "statusDot";

    public static final String ALERT_ICON = "alertIcon";
    public static final String ALERT_TEXT = "alertText";
    public static final String ALERT_BG = "alertBg";
    public static final String ALERT_BORDER = "alertBorder";
    public static final String ALERT_COLOR = "alertColor";
}

