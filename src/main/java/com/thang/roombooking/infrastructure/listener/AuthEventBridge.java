package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.event.OtpRequestedEvent;
import com.thang.roombooking.common.event.UserForgotPasswordEvent;
import com.thang.roombooking.common.event.UserPasswordResetSuccessEvent;
import com.thang.roombooking.infrastructure.configuration.RoomBookingRabbitMQProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuthEventBridge {

    private final RabbitTemplate rabbitTemplate;
    private final RoomBookingRabbitMQProperties properties;


    @EventListener
    @Async
    public void pushSendOtpCodeEventToRabbitMQ(OtpRequestedEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKeys().getEmailSecurityOtp(),
                event
        );
    }

    @EventListener
    @Async
    public void pushSendEmailResetPassToRabbitMQ(UserPasswordResetSuccessEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKeys().getEmailSecurityReset(),
                event
        );
    }

    @EventListener
    @Async
    public void pushSendPasswordResetEmailRequested(UserForgotPasswordEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKeys().getEmailSecurityOtp(),
                event
        );
    }


}
