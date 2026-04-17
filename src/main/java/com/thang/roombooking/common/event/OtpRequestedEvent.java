package com.thang.roombooking.common.event;

/**
 * Notification event for sending an OTP to a user email.
 */
public record OtpRequestedEvent(
        String email,
        String otpCode
) {}

