package com.thang.roombooking.infrastructure.mail.booking;

import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.infrastructure.configuration.AppProperties;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.mail.EmailI18nKeys;
import com.thang.roombooking.infrastructure.mail.MailTemplateNames;
import com.thang.roombooking.infrastructure.mail.TemplateVarKeys;
import com.thang.roombooking.infrastructure.mail.core.MailSender;
import com.thang.roombooking.infrastructure.mail.core.TemplateRenderer;
import com.thang.roombooking.service.notification.BookingEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailNotifierImpl implements BookingEmailNotifier {

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

    private String safeUserEmail(Booking booking) {
        return booking != null && booking.getUser() != null ? booking.getUser().getEmail() : null;
    }

    private String safeUserName(Booking booking) {
        return booking != null && booking.getUser() != null ? booking.getUser().getFullName() : null;
    }

    private String safeRoomName(Booking booking) {
        if (booking == null || booking.getClassroom() == null) return null;
        return booking.getClassroom().getRoomName();
    }

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void bookingCreatedPending(Booking booking) {
        String to = safeUserEmail(booking);
        if (to == null) return;

        Map<String, Object> vars = baseVars(booking);
        vars.put(TemplateVarKeys.HEADER_BG, BookingEmailDesign.COLOR_BLUE);
        vars.put(TemplateVarKeys.TITLE, I18nUtils.get(EmailI18nKeys.BOOKING_CREATED_PENDING_TITLE));
        vars.put(TemplateVarKeys.SUBTITLE, I18nUtils.get(EmailI18nKeys.BOOKING_CREATED_PENDING_SUBTITLE));
        vars.put(TemplateVarKeys.INTRO, I18nUtils.get(EmailI18nKeys.BOOKING_CREATED_PENDING_MESSAGE));

        vars.put(TemplateVarKeys.STATUS_TEXT, BookingEmailDesign.STATUS_PENDING);
        vars.put(TemplateVarKeys.STATUS_BG, BookingEmailDesign.STATUS_BG_PURPLE_TINT);
        vars.put(TemplateVarKeys.STATUS_COLOR, BookingEmailDesign.STATUS_PURPLE);
        vars.put(TemplateVarKeys.STATUS_DOT, BookingEmailDesign.STATUS_PURPLE);

        send(
                to,
                I18nUtils.get(EmailI18nKeys.BOOKING_CREATED_PENDING_SUBJECT, appProperties.getName()),
                MailTemplateNames.BOOKING_NOTIFICATION,
                vars
        );
    }

    @Override
    public void bookingStatusChanged(Booking booking, BookingStatus statusAfter) {
        String to = safeUserEmail(booking);
        if (to == null) return;

        Map<String, Object> vars = baseVars(booking);
        vars.put(TemplateVarKeys.HEADER_BG, BookingEmailDesign.COLOR_GREEN);
        vars.put(TemplateVarKeys.TITLE, I18nUtils.get(EmailI18nKeys.BOOKING_STATUS_CHANGED_TITLE));
        vars.put(TemplateVarKeys.SUBTITLE, I18nUtils.get(EmailI18nKeys.BOOKING_STATUS_CHANGED_SUBTITLE));
        vars.put(TemplateVarKeys.INTRO, I18nUtils.get(EmailI18nKeys.BOOKING_STATUS_CHANGED_MESSAGE));

        String statusText = statusAfter != null ? statusAfter.name() : "UPDATED";
        vars.put(TemplateVarKeys.STATUS_TEXT, statusText);
        vars.put(TemplateVarKeys.STATUS_BG, BookingEmailDesign.STATUS_BG_GREEN_TINT);
        vars.put(TemplateVarKeys.STATUS_COLOR, BookingEmailDesign.COLOR_GREEN);
        vars.put(TemplateVarKeys.STATUS_DOT, BookingEmailDesign.COLOR_GREEN);

        String subject = I18nUtils.get(EmailI18nKeys.BOOKING_STATUS_CHANGED_SUBJECT, appProperties.getName());
        send(to, subject, MailTemplateNames.BOOKING_NOTIFICATION, vars);
    }

    @Override
    public void roomAvailabilityUpdated(Long classroomId) {
        // This use-case often targets a list of subscribers; leave as a no-op until subscription model exists.
        log.info("Room availability updated for classroomId={}", classroomId);
    }

    @Override
    public void concurrentBookingDetected(Booking booking) {
        String to = safeUserEmail(booking);
        if (to == null) return;

        Map<String, Object> vars = baseVars(booking);
        vars.put(TemplateVarKeys.HEADER_BG, BookingEmailDesign.COLOR_ORANGE);
        vars.put(TemplateVarKeys.TITLE, I18nUtils.get(EmailI18nKeys.BOOKING_CONCURRENT_TITLE));
        vars.put(TemplateVarKeys.SUBTITLE, I18nUtils.get(EmailI18nKeys.BOOKING_CONCURRENT_SUBTITLE));
        vars.put(TemplateVarKeys.INTRO, I18nUtils.get(EmailI18nKeys.BOOKING_CONCURRENT_MESSAGE));

        vars.put(TemplateVarKeys.ALERT_ICON, "!");
        vars.put(TemplateVarKeys.ALERT_TEXT, I18nUtils.get(EmailI18nKeys.BOOKING_CONCURRENT_ALERT));
        vars.put(TemplateVarKeys.ALERT_BG, BookingEmailDesign.ALERT_BG_ORANGE_TINT);
        vars.put(TemplateVarKeys.ALERT_BORDER, BookingEmailDesign.ALERT_BORDER_ORANGE);
        vars.put(TemplateVarKeys.ALERT_COLOR, BookingEmailDesign.ALERT_COLOR_ORANGE);

        send(
                to,
                I18nUtils.get(EmailI18nKeys.BOOKING_CONCURRENT_SUBJECT, appProperties.getName()),
                MailTemplateNames.BOOKING_NOTIFICATION,
                vars
        );
    }

    @Override
    public void bookingCancelled(Booking booking) {
        String to = safeUserEmail(booking);
        if (to == null) return;

        Map<String, Object> vars = baseVars(booking);
        vars.put(TemplateVarKeys.HEADER_BG, BookingEmailDesign.COLOR_RED);
        vars.put(TemplateVarKeys.TITLE, I18nUtils.get(EmailI18nKeys.BOOKING_CANCELLED_TITLE));
        vars.put(TemplateVarKeys.SUBTITLE, I18nUtils.get(EmailI18nKeys.BOOKING_CANCELLED_SUBTITLE));
        vars.put(TemplateVarKeys.INTRO, I18nUtils.get(EmailI18nKeys.BOOKING_CANCELLED_MESSAGE));

        vars.put(TemplateVarKeys.STATUS_TEXT, BookingEmailDesign.STATUS_CANCELLED);
        vars.put(TemplateVarKeys.STATUS_BG, BookingEmailDesign.ALERT_BG_RED_TINT);
        vars.put(TemplateVarKeys.STATUS_COLOR, BookingEmailDesign.COLOR_RED);
        vars.put(TemplateVarKeys.STATUS_DOT, BookingEmailDesign.COLOR_RED);

        if (booking != null && booking.getCancelledBy() != null && !booking.getCancelledBy().isBlank()) {
            vars.put(TemplateVarKeys.ALERT_ICON, "i");
            vars.put(TemplateVarKeys.ALERT_TEXT, I18nUtils.get(EmailI18nKeys.BOOKING_CANCELLED_CANCELLED_BY, booking.getCancelledBy()));
            vars.put(TemplateVarKeys.ALERT_BG, BookingEmailDesign.ALERT_BG_RED_TINT);
            vars.put(TemplateVarKeys.ALERT_BORDER, BookingEmailDesign.ALERT_BORDER_RED);
            vars.put(TemplateVarKeys.ALERT_COLOR, BookingEmailDesign.ALERT_COLOR_RED);
        }

        send(
                to,
                I18nUtils.get(EmailI18nKeys.BOOKING_CANCELLED_SUBJECT, appProperties.getName()),
                MailTemplateNames.BOOKING_NOTIFICATION,
                vars
        );
    }

    private Map<String, Object> baseVars(Booking booking) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(TemplateVarKeys.APP_NAME, appProperties.getName());
        vars.put(TemplateVarKeys.SUPPORT_EMAIL, appProperties.getSupport().getEmail());

        vars.put(TemplateVarKeys.USER_NAME, safeUserName(booking));
        vars.put(TemplateVarKeys.BOOKING_ID, booking != null ? booking.getId() : null);
        vars.put(TemplateVarKeys.ROOM_NAME, safeRoomName(booking));
        vars.put(TemplateVarKeys.BOOKING_DATE, booking != null ? booking.getBookingDate() : null);
        vars.put(TemplateVarKeys.START_TIME, booking != null && booking.getStartTime() != null ? TIME_FMT.format(booking.getStartTime()) : null);
        vars.put(TemplateVarKeys.END_TIME, booking != null && booking.getEndTime() != null ? TIME_FMT.format(booking.getEndTime()) : null);
        vars.put(TemplateVarKeys.ATTENDEES, booking != null ? booking.getAttendees() : null);
        vars.put(TemplateVarKeys.PURPOSE, booking != null ? booking.getPurpose() : null);
        return vars;
    }

    private void send(String to, String subject, String template, Map<String, Object> vars) {
        String html = templateRenderer.render(template, vars);
        mailSender.sendHtml(to, subject, html, appProperties.getName(), effectiveFromEmail());
    }
}

