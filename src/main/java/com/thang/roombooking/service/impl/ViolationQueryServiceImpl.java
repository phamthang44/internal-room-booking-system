package com.thang.roombooking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.dto.response.ViolationResponse;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.ViolationQueryService;
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
public class ViolationQueryServiceImpl implements ViolationQueryService {

    private final BookingViolationRepository violationRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<ViolationResponse> getUserViolationHistory(Long userId, Pageable pageable) {
        Page<BookingViolation> violations = violationRepository.findAllByUserId(userId, pageable);
        
        return violations.map(v -> ViolationResponse.builder()
                .id(v.getId())
                .userId(v.getUser().getId())
                .bookingId(v.getBooking() != null ? v.getBooking().getId() : null)
                .type(v.getType())
                .source(v.getSource())
                .severityPoints(v.getSeverityPoints())
                .reason(translateReason(v.getReason()))
                .createdAt(v.getCreatedAt())
                .build());
    }

    @SuppressWarnings("unchecked")
    private String translateReason(String reason) {
        if (reason == null || !reason.startsWith("{")) {
            return reason;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(reason, Map.class);
            String key = (String) map.get("key");
            List<Object> args = (List<Object>) map.get("args");
            if (key != null) {
                return I18nUtils.get(key, args != null ? args.toArray() : new Object[0]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse/translate violation reason in query service: {}", reason);
        }
        return reason;
    }
}
