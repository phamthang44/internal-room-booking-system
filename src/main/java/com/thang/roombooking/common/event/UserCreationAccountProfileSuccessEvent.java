package com.thang.roombooking.common.event;

/**
 * Notification event for sending a welcome email after account creation.
 */
public record UserCreationAccountProfileSuccessEvent(
        String email,
        String fullName
) {}

