package com.thang.roombooking.common.event;

import java.time.Instant;

/**
 * Notification event for sending a password reset success email.
 */
public record UserPasswordResetSuccessEvent(
        String email,
        String username,
        Instant resetTimestamp
) {}

