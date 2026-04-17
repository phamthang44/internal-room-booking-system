package com.thang.roombooking.common.constant;

public class RabbitMQConstants {
    private RabbitMQConstants() {}

    public static final String EXCHANGE_NAME = "roombooking.core.exchange";

    public static final String RK_EMAIL_BOOKING_CREATED = "notification.email.booking.created";
    public static final String RK_EMAIL_BOOKING_APPROVED = "notification.email.booking.approved";
    public static final String RK_EMAIL_BOOKING_REJECTED = "notification.email.booking.rejected";
    public static final String RK_EMAIL_BOOKING_CANCELLED = "notification.email.booking.cancelled";
}