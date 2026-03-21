package com.thang.roombooking.infrastructure.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.CommonErrorCode;
import com.thang.roombooking.infrastructure.idempotency.entity.IdempotencyKey;
import com.thang.roombooking.infrastructure.idempotency.repository.IdempotencyRepository;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {
    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper; // Dùng để biến Request thành chuỗi để băm

    @Override
    public void validate(String key, Object requestBody) {
        String fingerprint = generateFingerprint(requestBody);

        var existingKey = repository.findByKeyHash(key);
        if (existingKey.isPresent()) {
            IdempotencyKey idempotencyKey = existingKey.get();
            // Nếu vân tay khác nhau -> Có người đang dùng lặp Key cho nội dung khác
            if (!idempotencyKey.getRequestFingerprint().equals(fingerprint)) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.idempotency_key_used"));
            }
            if (idempotencyKey.getReference() == null) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.idempotency_request_processing"));
            }
        } else {
            // Lưu mới với trạng thái chưa có kết quả (tương đương PROCESSING)
            repository.save(IdempotencyKey.builder()
                .keyHash(key)
                .requestFingerprint(fingerprint)
                .expiresAt(Instant.now().plusSeconds(3600)) // Hết hạn sau 1h
                .build());
        }
    }

    @Override
    public void saveResponse(String key, Object response) {
        repository.findByKeyHash(key).ifPresent(idempotencyKey -> {
            try {
                // Lưu kết quả xử lý vào cột reference (hoặc bạn có thể tạo thêm cột response_body)
                idempotencyKey.setReference(objectMapper.writeValueAsString(response));
                repository.save(idempotencyKey);
            } catch (Exception _) {
                throw new AppException(CommonErrorCode.INTERNAL_ERROR, I18nUtils.get("error.idempotency_save_failed"));
            }
        });
    }

    @Override
    public Object getStoredResponse(String key) {
        // Bạn sẽ cần logic để biến chuỗi JSON từ reference ngược lại thành Object
        // Tạm thời mình để logic trả về String/Object cơ bản
        return repository.findByKeyHash(key).map(IdempotencyKey::getReference).orElse(null);
    }

    private String generateFingerprint(Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            return DigestUtils.md5DigestAsHex(json.getBytes()); // Băm MD5 tạo vân tay
        } catch (Exception _) {
            return "default_fingerprint";
        }
    }
}
