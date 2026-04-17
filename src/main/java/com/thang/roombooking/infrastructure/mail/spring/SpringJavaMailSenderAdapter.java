package com.thang.roombooking.infrastructure.mail.spring;

import com.thang.roombooking.infrastructure.mail.core.MailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringJavaMailSenderAdapter implements MailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void sendText(String to, String subject, String textBody, String from) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(textBody);
        javaMailSender.send(message);
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody, String fromName, String fromEmail) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (fromEmail != null && !fromEmail.isBlank()) {
                if (fromName != null && !fromName.isBlank()) helper.setFrom(fromName + " <" + fromEmail + ">");
                else helper.setFrom(fromEmail);
            }

            javaMailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}", to, e);
        }
    }
}

