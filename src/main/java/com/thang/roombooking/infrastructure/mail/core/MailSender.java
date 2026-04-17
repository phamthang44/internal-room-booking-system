package com.thang.roombooking.infrastructure.mail.core;

/**
 * Minimal outbound mail port (DIP). Implementations can use SMTP, 3rd-party APIs, etc.
 */
public interface MailSender {

    void sendText(String to, String subject, String textBody, String from);

    void sendHtml(String to, String subject, String htmlBody, String fromName, String fromEmail);
}

