package com.thang.roombooking.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.exc.InputCoercionException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class GlobalHandlerError {

    // --- Business Logic Errors ---
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResult<?>> handleAppException(AppException e) {
        String traceId = getTraceId();
        log.warn("Business error [{}]: {} - Code: {}", traceId, e.getMessage(), e.getErrorCode());

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResult.error(
                        e.getErrorCode().getCode(),
                        e.getMessage(),
                        traceId
                ));
    }

    // --- Validation Errors (400) ---
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResult<?>> handleValidationException(Exception e) {
        String traceId = getTraceId();
        log.warn("Validation error [{}]: {}", traceId, e.getMessage());

        Object errorDetails = null;
        String message = I18nUtils.get("error.validation_failed");

        if (e instanceof MethodArgumentNotValidException ex) {
            BindingResult result = ex.getBindingResult();
            Map<String, String> errors = new HashMap<>();
            String firstErrorMessage = null;

            for (FieldError fieldError : result.getFieldErrors()) {
                String errorMessage = resolveValidationMessage(fieldError.getDefaultMessage());
                errors.put(fieldError.getField(), errorMessage);
                if (firstErrorMessage == null) {
                    firstErrorMessage = errorMessage;
                }
            }
            errorDetails = errors;
            message = (firstErrorMessage != null) ? firstErrorMessage : I18nUtils.get("error.invalid_input_data");
        } else if (e instanceof ConstraintViolationException ex) {
            errorDetails = ex.getConstraintViolations().stream()
                    .collect(Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(),
                            violation -> resolveValidationMessage(violation.getMessage()),
                            (msg1, msg2) -> msg1
                    ));
            message = I18nUtils.get("error.invalid_parameters");
        } else if (e instanceof MissingServletRequestParameterException ex) {
            message = I18nUtils.get("error.missing_required_parameter", ex.getParameterName());
        } else if (e instanceof IllegalArgumentException ex) {
            message = ex.getMessage();
        }

        return ResponseEntity.status(BAD_REQUEST)
                .body(ApiResult.error(
                        CommonErrorCode.INVALID_REQUEST.getCode(),
                        message,
                        traceId,
                        errorDetails
                ));
    }

    // --- Authentication Errors (401) ---
    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class,
            TokenExpiredException.class,
            TokenErrorException.class
    })
    public ResponseEntity<ApiResult<?>> handleAuthException(Exception e) {
        String traceId = getTraceId();
        log.warn("Auth error [{}]: {}", traceId, e.getMessage());

        return ResponseEntity.status(UNAUTHORIZED)
                .body(ApiResult.error(
                        AuthErrorCode.UNAUTHENTICATED.getCode(),
                        e.getMessage(),
                        traceId
                ));
    }

    // --- Access Denied (403) ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<?>> handleAccessDeniedException(AccessDeniedException e) {
        String traceId = getTraceId();
        log.warn("Access denied [{}]: {}", traceId, e.getMessage());

        return ResponseEntity.status(FORBIDDEN)
                .body(ApiResult.error(
                        AuthErrorCode.ACCESS_DENIED.getCode(),
                        AuthErrorCode.ACCESS_DENIED.format(),
                        traceId
                ));
    }

    // --- Not Found (404) ---
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResult<?>> handleNotFoundException(HttpServletRequest request, Exception e) {
        String traceId = getTraceId();
        String wrongUrl = request.getRequestURI();
        return ResponseEntity.status(NOT_FOUND)
                .body(ApiResult.error(
                        CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                        CommonErrorCode.RESOURCE_NOT_FOUND.format(wrongUrl),
                        traceId
                ));
    }

    // --- Method Not Allowed (405) ---
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        String traceId = getTraceId();
        return ResponseEntity.status(METHOD_NOT_ALLOWED)
                .body(ApiResult.error(
                        CommonErrorCode.METHOD_NOT_ALLOWED.getCode(),
                        e.getMessage(),
                        traceId
                ));
    }

    // --- Catch-All (500) ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleGlobalException(Exception e) {
        String traceId = getTraceId();
        log.error("Internal Server Error [{}]: ", traceId, e);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(
                        CommonErrorCode.INTERNAL_ERROR.getCode(),
                        I18nUtils.get("error.unexpected_error_occurred"),
                        traceId
                ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResult<?>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.error("Conflict detected: Another admin has already processed this booking.");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(
                        BookingErrorCode.BOOKING_ALREADY_PROCESSED.getCode(),
                        I18nUtils.get(BookingErrorCode.BOOKING_ALREADY_PROCESSED.getMessage()),
                        getTraceId()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        Throwable root = e.getMostSpecificCause();
        String traceId = getTraceId();
        String message = I18nUtils.get("error.invalid_format_request");
        String field = "unknown";

        // 1. Xử lý lỗi tràn số (Overflow) hoặc ép kiểu số
        if (root instanceof InputCoercionException ice) {
            field = extractFieldFromPath(ice);
            message = I18nUtils.get("error.numeric.out.of.range", field);
        }
        // 2. Xử lý lỗi sai định dạng (ví dụ: nhập "abc" vào ô "capacity")
        else if (root instanceof InvalidFormatException ife) {
            field = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));

            if (Number.class.isAssignableFrom(ife.getTargetType()) || ife.getTargetType().isPrimitive()) {
                message = I18nUtils.get("error.numeric.out.of.range", field);
            } else {
                message = I18nUtils.get("error.invalid_format_for_field", field);
            }
        }
        // 3. JSON hỏng (thiếu dấu ngoặc, phẩy...)
        else if (root instanceof com.fasterxml.jackson.core.JsonParseException) {
            message = I18nUtils.get("error.malformed_json_request");
        }

        log.warn("JSON Reading Error [{}]: {} at field [{}]", traceId, root.getMessage(), field);

        return ResponseEntity.status(BAD_REQUEST)
                .body(ApiResult.error(CommonErrorCode.INVALID_REQUEST.getCode(), message, traceId));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";

        // Kiểm tra xem có đúng là vi phạm cái Constraint "exclude_booking_overlap" không
        if (rootMsg.contains("exclude_booking_overlap")) {
            log.warn("{} | Overlap detected by DB Constraint |", LogConstant.BIZ_ERROR);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResult.error(BookingErrorCode.BOOKING_SLOT_OVERLAP));
        }

        // Các lỗi Integrity khác (ví dụ Foreign Key, Unique...)
        log.error("{} | Data Integrity Error: {}", LogConstant.SYS_ERROR, rootMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(CommonErrorCode.DATA_INTEGRITY_ERROR, ex.getMessage()));
    }

    private String extractFieldFromPath(Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) return null;

        Pattern p = Pattern.compile("\\[\"(.*?)\"]");
        Matcher m = p.matcher(msg);
        if (m.find()) return m.group(1);

        return null;
    }

    private String getTraceId() {
        return UUID.randomUUID().toString();
    }

    private String resolveValidationMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.startsWith("{") && message.endsWith("}")) {
            String key = message.substring(1, message.length() - 1);
            try {
                return I18nUtils.get(key);
            } catch (Exception e) {
                return message;
            }
        }
        if (!message.contains(" ") && message.contains(".")) {
            try {
                return I18nUtils.get(message);
            } catch (Exception e) {
                return message;
            }
        }
        return message;
    }
}
