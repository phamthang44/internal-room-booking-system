package com.thang.roombooking.infrastructure.listener;

import com.thang.roombooking.common.event.OtpRequestedEvent;
import com.thang.roombooking.common.event.UserCreationAccountProfileSuccessEvent;
import com.thang.roombooking.common.event.UserForgotPasswordEvent;
import com.thang.roombooking.common.event.UserPasswordResetSuccessEvent;
import com.thang.roombooking.infrastructure.configuration.AppProperties;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.mail.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component // Bean của Spring
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService; // Inject cái service gửi mail có sẵn của bạn vào đây
    private final AppProperties appProperties;

    private static final DateTimeFormatter RESET_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (O)").withZone(ZoneId.systemDefault());

//    @EventListener
//    @Async // Quan trọng: Chạy luồng riêng để không bắt User chờ
    @RabbitListener(queues = "${roombooking.rabbitmq.queues.email-priority}")
    public void handleOtpRequested(OtpRequestedEvent event) throws MessagingException {
        log.info("Catch event send OTP to: {}", event.email());

        emailService.sendEmailOtpCodeVerify(
                event.email(),
                I18nUtils.get("message.otp_subject"),
                event.otpCode()
        );
    }

//    @EventListener
//    @Async
    @RabbitListener(queues = "${roombooking.rabbitmq.queues.email-normal}")
    public void handleWelcomeEmailRequested(UserCreationAccountProfileSuccessEvent event) throws MessagingException {
        log.info("Catch event send Welcome Email to: {}", event.email());

        emailService.sendEmailRegistrationHtml(
                event.email(),
                event.fullName(),
                appProperties.getFrontendUrl()
        );
    }

    @RabbitListener(queues = "${roombooking.rabbitmq.queues.email-priority}")
    public void handlePasswordResetEmailRequested(UserForgotPasswordEvent event) throws MessagingException {
        log.info("Catch event send Password Reset Email to: {}", event.email());

        emailService.sendResetPasswordEmail(
                event.email(),
                I18nUtils.get("message.otp_subject_request_reset_password"),
                event.otpCode()
        );
    }

    @RabbitListener(queues = "${roombooking.rabbitmq.queues.email-priority}")
    public void handlePasswordResetSuccessEmailRequested(UserPasswordResetSuccessEvent event) throws MessagingException {
        log.info("Catch event send Password Reset Success Email to: {}", event.email());

        String resetTimeStamp = event.resetTimestamp() != null ? RESET_TS_FMT.format(event.resetTimestamp()) : "—";

        emailService.sendPasswordResetSuccessEmail(
                event.email(),
                event.username(),
                event.email(),
                resetTimeStamp,
                appProperties.getFrontendUrl() + "/login"
        );
    }
}
