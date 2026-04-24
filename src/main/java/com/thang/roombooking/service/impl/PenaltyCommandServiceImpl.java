package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.PenaltyExtendRequest;
import com.thang.roombooking.common.dto.request.PenaltyRevokeRequest;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.common.enums.PenaltyAction;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.PenaltyErrorCode;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.service.PenaltyCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class PenaltyCommandServiceImpl implements PenaltyCommandService {

    private final PenaltyRecordRepository penaltyRecordRepository;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public PenaltyRecordResponse revokePenalty(Long id, PenaltyRevokeRequest request, UserAccount admin) {
        log.info("{} | Admin {} is revoking penalty ID: {}", LogConstant.ACTION_START, admin.getEmail(), id);

        PenaltyRecord penalty = penaltyRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(PenaltyErrorCode.PENALTY_NOT_FOUND, id));

        if (!penalty.isActive()) {
            throw new AppException(PenaltyErrorCode.ALREADY_INACTIVE, id);
        }

        penalty.setActive(false);
        penalty.setPenaltyAction(PenaltyAction.REVOKED);

        return getPenaltyRecordResponse(admin, penalty, request.reason(), "penalty.append.revoked_by");
    }

    private PenaltyRecordResponse getPenaltyRecordResponse(UserAccount admin, PenaltyRecord penalty, String reason, String actionKey) {
        String appendReason = appendReason(reason, admin, actionKey);
        penalty.setReason(penalty.getReason() + appendReason);

        PenaltyRecord saved = penaltyRecordRepository.save(penalty);

        return PenaltyRecordResponse.builder()
                .id(saved.getId())
                .isActive(saved.isActive())
                .action(saved.getPenaltyAction())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .reason(saved.getReason())
                .userBasicResponse(UserBasicResponse.fromEntity(saved.getUser()))
                .build();
    }

    @Override
    @Transactional
    public PenaltyRecordResponse extendPenalty(Long id, PenaltyExtendRequest request, UserAccount admin) {
        log.info("{} | Admin {} is extending penalty ID: {} to {}", LogConstant.ACTION_START, admin.getEmail(), id, request.newEndDate());
 
        PenaltyRecord penalty = penaltyRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(PenaltyErrorCode.PENALTY_NOT_FOUND, id));
 
        if (!penalty.isActive()) {
            throw new AppException(PenaltyErrorCode.ALREADY_INACTIVE, id);
        }
 
        if (request.newEndDate().isBefore(penalty.getEndDate())) {
            throw new AppException(PenaltyErrorCode.INVALID_EXTEND_DATE);
        }
 
        penalty.setEndDate(request.newEndDate());

        return getPenaltyRecordResponse(admin, penalty, request.reason(), "penalty.append.extended_by");
    }

    private String appendReason(String reason, UserAccount admin, String actionKey) {
        Locale locale = LocaleContextHolder.getLocale();
        String appendReason = messageSource.getMessage(actionKey, new Object[]{admin.getEmail()}, locale);
        if (reason != null && !reason.isBlank()) {
            appendReason += messageSource.getMessage("penalty.append.reason", new Object[]{reason}, locale);
        }
        return appendReason;
    }



}
