package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PenaltyQueryService {
    Page<PenaltyRecordResponse> getUserPenaltyHistory(Long userId, Pageable pageable);
}
