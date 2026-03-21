package com.thang.roombooking.infrastructure.idempotency.config;


import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.CommonErrorCode;
import com.thang.roombooking.infrastructure.idempotency.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService service;
    private final HttpServletRequest request;

    @Around("@annotation(idempotentConfig)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotentConfig) throws Throwable {
        String key = request.getHeader("X-Idempotency-Key");

        // Nếu không có key thì cho qua luôn (hoặc bắt buộc tùy bạn)
        if (key == null || key.isBlank()) return joinPoint.proceed();

        // 1. Kiểm tra "Quyển sổ"
        Object storedResponse = service.getStoredResponse(key);
        if (storedResponse != null) {
            return storedResponse; // Trả về kết quả cũ ngay lập tức
        }

        // 2. Validate và đánh dấu PROCESSING
        // Lấy tham số đầu tiên của hàm (thường là Request Body) để làm vân tay
        Object requestBody = joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null;
        service.validate(key, requestBody);

        try {
            // 3. Cho phép hàm chính (Checkout/Thanh toán) chạy
            Object result = joinPoint.proceed();

            // 4. Lưu kết quả thành công
            service.saveResponse(key, result);
            return result;
        } catch (Exception _) {
            // Nếu lỗi thì có thể xóa key để user thử lại
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "");
        }
    }
}
