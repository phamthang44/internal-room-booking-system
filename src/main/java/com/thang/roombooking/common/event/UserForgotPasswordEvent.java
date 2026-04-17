package com.thang.roombooking.common.event;

/**
 * Notification event for sending a password reset OTP.
 */
public record UserForgotPasswordEvent(
        String email,
        String otpCode
) {}

