package com.thang.roombooking.infrastructure.messaging;

import com.thang.roombooking.common.constant.RabbitMQConstants;
import com.thang.roombooking.common.enums.BookingAction;
import com.thang.roombooking.common.event.BookingNotificationRequestedEvent;
import com.thang.roombooking.common.event.BookingStatusChangedEvent;
import com.thang.roombooking.infrastructure.configuration.RoomBookingRabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingStatusChangedRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RoomBookingRabbitMQProperties rabbitMQProperties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        if (event == null || event.booking() == null || event.booking().getId() == null) return;

        BookingAction action;
        try {
            action = event.action() != null ? BookingAction.valueOf(event.action()) : null;
        } catch (IllegalArgumentException ex) {
            log.warn("Skip publish: unknown booking action '{}'", event.action());
            return;
        }
        if (action == null) return;

        String routingKey = switch (action) {
            case CREATE_BOOKING -> RabbitMQConstants.RK_EMAIL_BOOKING_CREATED;
            case APPROVE_BOOKING -> RabbitMQConstants.RK_EMAIL_BOOKING_APPROVED;
            case REJECT_BOOKING, SYSTEM_REJECT -> RabbitMQConstants.RK_EMAIL_BOOKING_REJECTED;
            case CANCEL_BOOKING -> RabbitMQConstants.RK_EMAIL_BOOKING_CANCELLED;
            case CHECK_IN -> RabbitMQConstants.RK_EMAIL_BOOKING_CHECKIN;
            case CHECK_OUT -> RabbitMQConstants.RK_EMAIL_BOOKING_CHECKOUT;
            default -> null;
        };

        if (routingKey == null) return;

        BookingNotificationRequestedEvent payload = new BookingNotificationRequestedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                event.booking().getId(),
                event.action(),
                event.statusAfter(),
                event.performedBy(),
                event.note(),
                event.locale()
        );

        rabbitTemplate.convertAndSend(
                rabbitMQProperties.getExchange(),
                routingKey,
                payload
        );
    }
}

