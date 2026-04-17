package com.thang.roombooking.infrastructure.storage.cloudinary;

import com.cloudinary.utils.ObjectUtils;

import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.StorageErrorCode;
import com.thang.roombooking.infrastructure.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class CloudinaryStorageServiceImpl implements FileStorageService {

    private final CloudinaryClient cloudinaryClient;
    private final CloudinaryProperties cloudinaryProperties;

    public CloudinaryStorageServiceImpl(CloudinaryClient cloudinaryClient, CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryClient = cloudinaryClient;
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        validateFile(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> options = (Map<String, Object>) (Map<?, ?>) ObjectUtils.asMap(
                    "folder", cloudinaryProperties.getFolder(),
                    "resource_type", "auto",
                    "tags", "temporary" // <--- QUAN TRỌNG: Đánh dấu là rác trước
            );

            Map<String, Object> uploadResult =
                    cloudinaryClient.upload(file.getBytes(), options);

            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                throw new AppException(
                        StorageErrorCode.UPLOAD_NO_URL
                );
            }

            return secureUrl.toString();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new AppException(
                    StorageErrorCode.UPLOAD_FAILED
            );
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String publicId = extractPublicId(fileUrl);
            cloudinaryClient.delete(publicId);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Delete failed", e);
            throw new AppException(
                    StorageErrorCode.DELETE_FAILED
            );
        }
    }

    @Override
    public List<String> uploadMultipleFiles(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> options = (Map<String, Object>) (Map<?, ?>) ObjectUtils.asMap(
                        "folder", cloudinaryProperties.getFolder(),
                        "resource_type", "auto",
                        "tags", "temporary"
                );

                Map<String, Object> uploadResult =
                        cloudinaryClient.upload(file.getBytes(), options);

                Object secureUrl = uploadResult.get("secure_url");
                if (secureUrl == null) {
                    throw new AppException(
                            StorageErrorCode.UPLOAD_NO_URL
                    );
                }

                urls.add(secureUrl.toString());
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                log.error("Upload failed", e);
                throw new AppException(
                        StorageErrorCode.UPLOAD_FAILED
                );
            }
        }

        return urls;
    }

    @Override
    public void confirmFiles(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) return;

        // Chạy Async để không block luồng chính của ProductService
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Chuyển list URL thành list Public ID
                List<String> publicIds = fileUrls.stream()
                        .map(this::extractPublicId) // Tái sử dụng hàm extractPublicId ở dưới
                        .toList();

                // 2. Gọi client để gỡ tag
                cloudinaryClient.removeTag("temporary", publicIds);

                log.info("✅ Đã confirm {} ảnh (gỡ tag temporary).", publicIds.size());
            } catch (Exception e) {
                log.error("❌ Lỗi khi confirm ảnh: {}", e.getMessage());
                // Không throw exception ra ngoài vì đây là tác vụ phụ (background task)
            }
        });
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(
                    StorageErrorCode.FILE_INVALID
            );
        }

        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image/")) {
            throw new AppException(
                    StorageErrorCode.FILE_TYPE_NOT_ALLOWED
            );
        }
    }

    private String extractPublicId(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/")) {
            throw new AppException(
                    StorageErrorCode.FILE_URL_INVALID
            );
        }

        String[] parts = fileUrl.split("/");
        String fileName = parts[parts.length - 1];
        String folder = parts[parts.length - 2];

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0) {
            throw new AppException(
                    StorageErrorCode.FILE_URL_INVALID
            );
        }

        return folder + "/" + fileName.substring(0, dotIndex);
    }
}
