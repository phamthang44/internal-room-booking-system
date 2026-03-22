package com.thang.roombooking.infrastructure.idempotency.config;


import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.CommonErrorCode;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.idempotency.dto.IdempotencyResponseDTO;
import com.thang.roombooking.infrastructure.idempotency.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final IdempotencyService service;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper; // Bổ sung thiếu sót 1

    @Around("@annotation(idempotentConfig)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotentConfig) throws Throwable {
        String key = request.getHeader("X-Idempotency-Key");
        if (key == null || key.isBlank()) return joinPoint.proceed();

        String path = request.getRequestURI();
        Object requestBody = joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null;

        try {
            // 1. Kiểm tra/Tạo bản ghi nháp
            Optional<IdempotencyResponseDTO> storedResponse = service.validate(key, path, requestBody);

            if (storedResponse.isPresent()) {
                IdempotencyResponseDTO dto = storedResponse.get();
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();

                // Trả về kết quả cũ từ Snapshot
                return objectMapper.readValue(dto.body(), signature.getReturnType());
            }

            // 2. Thực thi Business Logic chính (Đặt phòng, v.v.)
            Object result = joinPoint.proceed();

            // 3. Lưu Snapshot thành công
            service.saveResponse(key, 200, result);
            return result;

        } catch (DataIntegrityViolationException _) {
            // Bắt lỗi khi 2 request cùng insert 1 Key Hash đồng thời
            throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.idempotency_request_processing"));
        }
    }
}
