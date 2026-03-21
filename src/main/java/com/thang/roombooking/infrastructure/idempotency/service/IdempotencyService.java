package com.thang.roombooking.infrastructure.idempotency.service;

public interface IdempotencyService {
    // Kiểm tra và lưu trạng thái tạm thời (PROCESSING)
    void validate(String key, Object requestBody);

    // Cập nhật kết quả sau khi xử lý thành công (COMPLETED)
    void saveResponse(String key, Object response);

    // Lấy lại kết quả cũ nếu đã tồn tại
    Object getStoredResponse(String key);
}
