package com.thang.roombooking.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thang.roombooking.common.exception.errorcode.BaseErrorCode;
import lombok.Builder;
import lombok.Getter;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    // 1. Dữ liệu chính
    private T data;

    // 2. Metadata
    private Meta meta;

    // 3. Lỗi
    private ErrorDetail error;

    // --- INNER CLASSES ---
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        // --- System Info (TỰ ĐỘNG GÁN) ---
        @Builder.Default
        private Long serverTime = System.currentTimeMillis(); // Tự lấy giờ hiện tại

        @Builder.Default
        private String apiVersion = "1.0.0"; // Version mặc định

        @Builder.Default
        private String traceId = java.util.UUID.randomUUID().toString(); // Mặc định random UUID

        // --- UI Feedback ---
        private String message;

        // --- Cursor Pagination (Newsfeed) ---
        private String nextCursor;
        private Boolean hasNextPage;

        // --- Offset Pagination (Admin) ---
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;

        // --- Sort & Filter ---
        private String sort;
        private Map<String, Object> filter;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private String traceId;
        private Object details; // Validation errors (Map hoặc List)
    }

    // --- FACTORY METHODS ---

    // 1. Thành công đơn giản
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .data(data)
                .meta(Meta.builder().build()) // serverTime sẽ tự có nhờ @Builder.Default
                .build();
    }

    // 2. Success có thông báo
    public static <T> ApiResult<T> success(T data, String message) {
        return ApiResult.<T>builder()
                .data(data)
                .meta(Meta.builder().message(message).build())
                .build();
    }

    // 3. Success Full Option (Search/Filter)
    public static <T> ApiResult<T> success(T data, Meta meta) {
        return ApiResult.<T>builder()
                .data(data)
                .meta(meta)
                .build();
    }

    // 4. Success Cursor (Newsfeed)
    public static <T> ApiResult<T> success(T data, String nextCursor, boolean hasNext) {
        return ApiResult.<T>builder()
                .data(data)
                .meta(Meta.builder()
                        .nextCursor(nextCursor)
                        .hasNextPage(hasNext)
                        .build())
                .build();
    }

    // 5. Success Offset (Admin)
    public static <T> ApiResult<List<T>> success(List<T> items, int page, int size, long total) {
        return ApiResult.<List<T>>builder()
                .data(items)
                .meta(Meta.builder()
                        .page(page)
                        .size(size)
                        .totalElements(total)
                        .totalPages((int) Math.ceil((double) total / size))
                        .build())
                .build();
    }

    // 6. Success from Spring Data Page
    public static <T> ApiResult<List<T>> successPage(Page<T> page) {
        return success(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // 7. Success from Spring Data Page with Message
    public static <T> ApiResult<List<T>> successPage(Page<T> page, String message) {
        return ApiResult.<List<T>>builder()
                .data(page.getContent())
                .meta(Meta.builder()
                        .message(message)
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    // 8. Success from Mapped Content and Page Info
    public static <R, T> ApiResult<List<R>> success(List<R> content, Page<T> pageInfo) {
        return ApiResult.<List<R>>builder()
                .data(content)
                .meta(Meta.builder()
                        .page(pageInfo.getNumber())
                        .size(pageInfo.getSize())
                        .totalElements(pageInfo.getTotalElements())
                        .totalPages(pageInfo.getTotalPages())
                        .build())
                .build();
    }

    // 9. Trả về Lỗi (Cơ bản)
    public static ApiResult<?> error(String code, String message, String traceId) {
        return ApiResult.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .traceId(traceId)
                        .build())
                .build();
    }

    // 10. Trả về Lỗi (Chi tiết - Dùng cho Validation)
    public static ApiResult<?> error(String code, String message, String traceId, Object details) {
        return ApiResult.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .traceId(traceId)
                        .details(details)
                        .build())
                .build();
    }

    public static <T> ApiResult<T> success() {
        return ApiResult.<T>builder()
                .meta(Meta.builder().build())
                .build();
    }

    // 11. Trả về Lỗi (Dùng BaseErrorCode)
    public static ApiResult<?> error(BaseErrorCode errorCode, Object... args) {
        String message = errorCode != null ? errorCode.format(args) : "Unknown error";
        String code = errorCode != null ? errorCode.getCode() : "UNKNOWN_ERROR";
        String traceId = java.util.UUID.randomUUID().toString();
        return ApiResult.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .traceId(traceId)
                        .build())
                .build();
    }


}
