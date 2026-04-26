package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.utils.PenaltyReasonUtils;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.service.PenaltyQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PenaltyQueryServiceImpl implements PenaltyQueryService {

    private final PenaltyRecordRepository penaltyRecordRepository;
    private final PenaltyReasonUtils penaltyReasonUtils;

    @Override
    public Page<PenaltyRecordResponse> getUserPenaltyHistory(Long userId, Pageable pageable) {
        Page<PenaltyRecord> penalties = penaltyRecordRepository.findAllByUserId(userId, pageable);
        
        return penalties.map(p -> PenaltyRecordResponse.builder()
                .id(p.getId())
                .isActive(p.isActive())
                .action(p.getPenaltyAction())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .reason(penaltyReasonUtils.translate(p.getReason()))
                .userBasicResponse(UserBasicResponse.fromEntity(p.getUser()))
                .build());
    }
}

