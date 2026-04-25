package com.thang.roombooking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.service.PenaltyQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PenaltyQueryServiceImpl implements PenaltyQueryService {

    private final PenaltyRecordRepository penaltyRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<PenaltyRecordResponse> getUserPenaltyHistory(Long userId, Pageable pageable) {
        Page<PenaltyRecord> penalties = penaltyRecordRepository.findAllByUserId(userId, pageable);
        
        return penalties.map(p -> PenaltyRecordResponse.builder()
                .id(p.getId())
                .isActive(p.isActive())
                .action(p.getPenaltyAction())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .reason(translateReason(p.getReason()))
                .userBasicResponse(UserBasicResponse.fromEntity(p.getUser()))
                .build());
    }

    private String translateReason(String reason) {
        if (reason == null) return null;

        // 1. Try parsing as JSON (structured automated reasons)
        if (reason.startsWith("{")) {
            try {
                Map<String, Object> map = objectMapper.readValue(reason, Map.class);
                String key = (String) map.get("key");
                List<Object> args = (List<Object>) map.get("args");
                if (key != null) {
                    return I18nUtils.get(key, args != null ? args.toArray() : new Object[0]);
                }
            } catch (Exception e) {
                log.warn("Failed to parse penalty reason JSON: {}", reason);
            }
        }

        // 2. Try translating as a plain key
        try {
            return I18nUtils.get(reason);
        } catch (Exception e) {
            // Not a translation key, return original string (e.g. manual admin note)
            return reason;
        }
    }
}
