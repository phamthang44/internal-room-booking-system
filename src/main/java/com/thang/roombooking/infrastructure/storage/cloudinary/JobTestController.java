package com.thang.roombooking.infrastructure.storage.cloudinary;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-jobs")
@RequiredArgsConstructor
@Profile("dev") // 👈 Quan trọng: Chỉ cho phép chạy ở môi trường Dev
public class JobTestController {

    private final CloudinaryCleanupJob cleanupJob; // Inject cái Job vào

    @PostMapping("/cleanup-cloudinary")
    public ResponseEntity<String> triggerCleanup() {
        // Gọi hàm trực tiếp cưỡng bức nó chạy
        cleanupJob.deleteOrphanedImages();
        return ResponseEntity.ok("Job triggered. Check logs for details.");
    }
}