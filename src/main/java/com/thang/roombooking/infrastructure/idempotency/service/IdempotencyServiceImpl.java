package com.thang.roombooking.infrastructure.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import com.thang.roombooking.infrastructure.idempotency.dto.IdempotencyResponseDTO;
import com.thang.roombooking.infrastructure.idempotency.entity.IdempotencyKey;
import com.thang.roombooking.infrastructure.idempotency.repository.IdempotencyRepository;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Optional<IdempotencyResponseDTO> validate(String key, String path, Object requestBody) {
        String fingerprint = generateFingerprint(requestBody);

        return repository.findByKeyHash(key).map(existing -> {
            // 1. Kiểm tra Resource Path: Chống việc dùng 1 Key cho nhiều API khác nhau
            if (!existing.getResourcePath().equals(path)) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.idempotency_key_mismatch_path"));
            }

            // 2. Kiểm tra Fingerprint: Chống việc dùng 1 Key cho Body khác nhau
            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.idempotency_key_used"));
            }

            // 3. Nếu đang xử lý (chưa có responseBody) -> Báo lỗi Request đang được thực hiện
            if (existing.getResponseBody() == null) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, I18nUtils.get("error.idempotency_request_processing"));
            }

            // 4. Nếu đã có kết quả -> Trả về DTO chứa Code và Body để Controller phản hồi ngay
            return IdempotencyResponseDTO.builder()
                    .statusCode(existing.getResponseCode())
                    .body(existing.getResponseBody())
                    .build();
        }).or(() -> {
            // 5. Nếu Key chưa tồn tại -> Lưu bản ghi nháp (Trạng thái PROCESSING)
            saveInitialKey(key, path, fingerprint);
            return Optional.empty();
        });
    }

    @Override
    @Transactional
    public void saveResponse(String key, int statusCode, Object response) {
        repository.findByKeyHash(key).ifPresent(entity -> {
            try {
                entity.setResponseCode(statusCode);
                entity.setResponseBody(objectMapper.writeValueAsString(response));
                repository.save(entity);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize response for idempotency key: {}", key, e);
            }
        });
    }

    private void saveInitialKey(String key, String path, String fingerprint) {
        IdempotencyKey entity = IdempotencyKey.builder()
                .keyHash(key)
                .resourcePath(path)
                .requestFingerprint(fingerprint)
                .expiresAt(Instant.now().plusSeconds(3600)) // Hết hạn sau 1h
                .build();
        repository.save(entity);
    }

    private String generateFingerprint(Object body) {
        try {
            return DigestUtils.md5DigestAsHex(objectMapper.writeValueAsBytes(body));
        } catch (Exception _) {
            return "empty_fingerprint";
        }
    }
}