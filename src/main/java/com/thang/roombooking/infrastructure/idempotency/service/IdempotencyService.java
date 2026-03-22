package com.thang.roombooking.infrastructure.idempotency.service;

import com.thang.roombooking.infrastructure.idempotency.dto.IdempotencyResponseDTO;

import java.util.Optional;

public interface IdempotencyService {
    // Kiểm tra và lưu trạng thái tạm thời (PROCESSING)
    Optional<IdempotencyResponseDTO> validate(String key, String path, Object requestBody);

    // Cập nhật kết quả sau khi xử lý thành công (COMPLETED)
    void saveResponse(String key, int statusCode, Object response);

}
