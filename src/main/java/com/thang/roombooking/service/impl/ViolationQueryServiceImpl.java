package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.ViolationResponse;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.service.ViolationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViolationQueryServiceImpl implements ViolationQueryService {

    private final BookingViolationRepository violationRepository;

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

    private String translateReason(String reason) {
        if (reason == null) return null;
        try {
            return I18nUtils.get(reason);
        } catch (Exception e) {
            // If it's not a valid key, return the original reason
            return reason;
        }
    }
}
