package com.thang.roombooking.controller;


import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<String>> upload(@RequestParam("file") MultipartFile file) {
        // 2. Upload
        String url = fileStorageService.uploadFile(file);

        // 3. Trả về URL cho Frontend
        return ResponseEntity.ok(ApiResult.success(url));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResult<List<String>>> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files
    ) {
        // 2. Gọi Service xử lý (trả về List URL)
        List<String> urls = fileStorageService.uploadMultipleFiles(files);

        // 3. Trả về
        return ResponseEntity.ok(ApiResult.success(urls));
    }

}
