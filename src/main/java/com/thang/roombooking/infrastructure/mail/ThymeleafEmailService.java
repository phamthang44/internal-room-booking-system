package com.thang.roombooking.infrastructure.mail;

import com.thang.roombooking.infrastructure.configuration.AppProperties;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.mail.core.MailSender;
import com.thang.roombooking.infrastructure.mail.core.TemplateRenderer;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThymeleafEmailService implements EmailService {

    private final MailSender mailSender;
    private final TemplateRenderer templateRenderer;
    private final AppProperties appProperties;

    @Value("${spring.mail.username:}")
    private String springMailUsername;

    private String effectiveFromEmail() {
        String configured = appProperties.getMail() != null ? appProperties.getMail().getFromEmail() : null;
        if (configured != null && !configured.isBlank()) return configured;
        if (springMailUsername != null && !springMailUsername.isBlank()) return springMailUsername;
        return null;
    }

    @Override
    public void sendRegistrationEmail(String toEmail, String subject, String body) {
        mailSender.sendText(toEmail, subject, body, effectiveFromEmail());
    }

    public void sendEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> variables) {
        String htmlContent = templateRenderer.render(templateName, variables);
        mailSender.sendHtml(to, subject, htmlContent, appProperties.getName(), effectiveFromEmail());
        log.info("Email sent to {} with template {}", to, templateName);
    }

    @Override
    public void sendEmailOtpCodeVerify(String toEmail, String subject, String code) throws MessagingException {
        sendOtpCodeEmail(
                toEmail,
                subject,
                code,
                EmailDefaults.OTP_EXPIRY_MINUTES,
                I18nUtils.get(EmailI18nKeys.OTP_VERIFY_MESSAGE)
        );
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String subject, String code) throws MessagingException {
        sendOtpCodeEmail(
                toEmail,
                subject,
                code,
                EmailDefaults.OTP_EXPIRY_MINUTES,
                I18nUtils.get(EmailI18nKeys.PASSWORD_RESET_OTP_MESSAGE)
        );
    }

    private void sendOtpCodeEmail(String toEmail, String subject, String code,
                                  int expiryMinutes, String customMessage) throws MessagingException {
        try {
            Map<String, Object> variables = Map.of(
                    TemplateVarKeys.APP_NAME, appProperties.getName(),
                    TemplateVarKeys.GREETING, I18nUtils.get(EmailI18nKeys.GREETING_DEFAULT),
                    TemplateVarKeys.MESSAGE, customMessage,
                    TemplateVarKeys.OTP_CODE, code,
                    TemplateVarKeys.EXPIRY_MINUTES, expiryMinutes,
                    TemplateVarKeys.SUPPORT_EMAIL, appProperties.getSupport().getEmail(),
                    TemplateVarKeys.HELP_CENTER_URL, appProperties.getUrl() + "/help",
                    TemplateVarKeys.PRIVACY_POLICY_URL, appProperties.getUrl() + "/privacy",
                    TemplateVarKeys.TERMS_URL, appProperties.getUrl() + "/terms"
            );

            sendEmailWithTemplate(toEmail, subject, MailTemplateNames.OTP_CODE, variables);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new MessagingException(I18nUtils.get(EmailI18nKeys.ERROR_EMAIL_SERVICE), e);
        }
    }

    @Override
    public void sendEmailRegistrationHtml(String toEmail, String username, String activationUrl)
            throws MessagingException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put(TemplateVarKeys.APP_NAME, appProperties.getName());
            variables.put(TemplateVarKeys.TAGLINE, appProperties.getTagline());
            variables.put(TemplateVarKeys.USER_NAME, username);
            variables.put(TemplateVarKeys.WELCOME_MESSAGE, I18nUtils.get(EmailI18nKeys.WELCOME_MESSAGE));

            variables.put(TemplateVarKeys.CTA_URL, activationUrl);
            variables.put(TemplateVarKeys.CTA_TEXT, I18nUtils.get(EmailI18nKeys.WELCOME_CTA));

            variables.put(TemplateVarKeys.SUPPORT_EMAIL, appProperties.getSupport().getEmail());
            variables.put(TemplateVarKeys.HELP_CENTER_URL, appProperties.getUrl() + "/help");
            variables.put(TemplateVarKeys.PRIVACY_POLICY_URL, appProperties.getUrl() + "/privacy");
            variables.put(TemplateVarKeys.UNSUBSCRIBE_URL, appProperties.getUrl() + "/unsubscribe");

            sendEmailWithTemplate(
                    toEmail,
                    I18nUtils.get(EmailI18nKeys.WELCOME_SUBJECT, appProperties.getName()),
                    MailTemplateNames.WELCOME,
                    variables
            );
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", toEmail, e);
            throw new MessagingException(I18nUtils.get(EmailI18nKeys.ERROR_EMAIL_SERVICE), e);
        }
    }

    @Override
    public void sendPasswordResetSuccessEmail(String toEmail, String userName, String userEmail,
                                              String resetTimestamp, String loginUrl) throws MessagingException {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put(TemplateVarKeys.APP_NAME, appProperties.getName());
            variables.put(TemplateVarKeys.USER_NAME, userName);
            variables.put(TemplateVarKeys.USER_EMAIL, userEmail != null ? userEmail : toEmail);
            variables.put(TemplateVarKeys.RESET_TIMESTAMP, resetTimestamp);

            variables.put(TemplateVarKeys.LOGIN_URL, loginUrl);
            variables.put(TemplateVarKeys.SUPPORT_EMAIL, appProperties.getSupport().getEmail());
            variables.put(TemplateVarKeys.HELP_CENTER_URL, appProperties.getUrl() + "/help");
            variables.put(TemplateVarKeys.PRIVACY_POLICY_URL, appProperties.getUrl() + "/privacy");
            variables.put(TemplateVarKeys.SECURITY_URL, appProperties.getUrl() + "/security");

            sendEmailWithTemplate(
                    toEmail,
                    I18nUtils.get(EmailI18nKeys.PASSWORD_RESET_SUCCESS_SUBJECT, appProperties.getName()),
                    MailTemplateNames.RESET_PASSWORD_SUCCESS,
                    variables
            );
        } catch (Exception e) {
            log.error("Failed to send password reset success email to {}", toEmail, e);
            throw new MessagingException(I18nUtils.get(EmailI18nKeys.ERROR_EMAIL_SERVICE), e);
        }
    }
}

