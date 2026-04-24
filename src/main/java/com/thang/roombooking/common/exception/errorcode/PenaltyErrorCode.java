package com.thang.roombooking.common.exception.errorcode;
 
 import com.thang.roombooking.infrastructure.i18n.I18nUtils;
 import lombok.Getter;
 import lombok.RequiredArgsConstructor;
 import org.springframework.http.HttpStatus;
 
 @Getter
 @RequiredArgsConstructor
 public enum PenaltyErrorCode implements BaseErrorCode {
     PENALTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PNL_404", "error.penalty.not_found"),
     ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, "PNL_001", "error.penalty.already_inactive"),
     INVALID_EXTEND_DATE(HttpStatus.BAD_REQUEST, "PNL_002", "error.penalty.invalid_extend_date"),
     ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "PNL_003", "error.penalty.already_expired");
 
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
