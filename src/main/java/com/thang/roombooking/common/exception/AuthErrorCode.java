package com.thang.roombooking.common.exception;

import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    // 401: Unauthenticated
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "AUTH_001", "error.authentication_required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "error.invalid_credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "error.token_expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_004", "error.token_invalid"),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "AUTH_005", "error.token_revoked"),

    // 403: Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_006", "error.access_denied"),

    // Auth Logic
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "AUTH_007", "error.account_locked"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "AUTH_008", "error.account_disabled"),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "AUTH_011", "error.account_banned"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "error.user_not_found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "error.user_already_exists"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "USER_003", "error.invalid_password_format"),
    USER_ALREADY_BANNED(HttpStatus.CONFLICT, "USER_004", "error.user_already_banned"),

    // Admin
    ACCOUNT_CANNOT_BANNED(HttpStatus.FORBIDDEN, "USER_005", "error.account_cannot_banned"),
    CANNOT_BAN_ADMIN(HttpStatus.CONFLICT, "AUTH_012", "error.cannot_ban_admin"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_013", "error.role_not_found"),

    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_005", "error.username_already_exists"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_006", "error.email_already_exists"),
    PASSWORDS_DO_NOT_MATCH(HttpStatus.BAD_REQUEST, "USER_007", "error.passwords_do_not_match"),
    INVALID_FULLNAME_FORMAT(HttpStatus.BAD_REQUEST, "USER_008", "error.invalid_fullname_format"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "USER_009", "error.invalid_email_format"),
    ACCOUNT_DOES_NOT_EXISTS(HttpStatus.NOT_FOUND, "AUTH_014", "error.account_does_not_exists"),;

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageKey;

    @Override
    public String getMessage() {
        return messageKey;
    }

    public String format(Object... args) {
        try {
            if (args == null || args.length == 0) {
                return I18nUtils.get(messageKey);
            }
            return I18nUtils.get(messageKey, args);
        } catch (Exception e) {
            return messageKey;
        }
    }
}
