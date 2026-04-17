package com.thang.roombooking.infrastructure.storage.cloudinary;

import com.cloudinary.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CloudinaryCleanupJob {

    private final CloudinaryClient cloudinaryClient;

    public CloudinaryCleanupJob(CloudinaryClient cloudinaryClient) {
        this.cloudinaryClient = cloudinaryClient;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteOrphanedImages() {
        log.info("🧹 Bắt đầu quét dọn ảnh rác trên Cloudinary...");

        try {
            // 1. Tìm các ảnh có tag "temporary" quá hạn
            ApiResponse result = cloudinaryClient.searchByTag("temporary");

            // 2. Parse kết quả để lấy danh sách public_id
            List<String> publicIds = extractPublicIds(result);

            // 3. Gọi lệnh xóa nếu có ảnh rác
            if (!publicIds.isEmpty()) {
                cloudinaryClient.deleteResources(publicIds);
                log.info("✅ Đã dọn dẹp {} ảnh rác thành công.", publicIds.size());
            } else {
                log.info("✨ Không tìm thấy ảnh rác nào.");
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi chạy job dọn dẹp Cloudinary", e);
        }
    }

    // Helper method: Trích xuất public_id từ response của Cloudinary
    private List<String> extractPublicIds(ApiResponse result) {
        List<String> publicIds = new ArrayList<>();

        // Cấu trúc JSON trả về của Search API:
        // { "resources": [ { "public_id": "abc", ... }, ... ] }
        if (result.containsKey("resources")) {
            List<Map<String, Object>> resources = (List<Map<String, Object>>) result.get("resources");

            for (Map<String, Object> resource : resources) {
                if (resource.containsKey("public_id")) {
                    publicIds.add((String) resource.get("public_id"));
                }
            }
        }
        return publicIds;
    }
}
