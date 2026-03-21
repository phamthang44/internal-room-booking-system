package com.thang.roombooking.common.exception;

import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
            Map<String, String> errors = ex.getConstraintViolations().stream()
                    .collect(Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(),
                            violation -> resolveValidationMessage(violation.getMessage()),
                            (msg1, msg2) -> msg1
                    ));
            errorDetails = errors;
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
        return message;
    }
}
