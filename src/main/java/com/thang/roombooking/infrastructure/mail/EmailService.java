package com.thang.roombooking.infrastructure.mail;

import jakarta.mail.MessagingException;
public interface EmailService {

    void sendRegistrationEmail(String toEmail, String subject, String body);

    void sendEmailOtpCodeVerify(String toEmail, String subject, String code) throws MessagingException;

    void sendResetPasswordEmail(String toEmail, String subject, String code) throws MessagingException;

    void sendEmailRegistrationHtml(String toEmail, String username, String activationUrl) throws MessagingException;

    void sendPasswordResetSuccessEmail(String toEmail, String userName, String userEmail,
                                       String resetTimestamp, String loginUrl) throws MessagingException;
}