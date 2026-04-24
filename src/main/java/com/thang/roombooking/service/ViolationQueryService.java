package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.ViolationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ViolationQueryService {
    Page<ViolationResponse> getUserViolationHistory(Long userId, Pageable pageable);
}
