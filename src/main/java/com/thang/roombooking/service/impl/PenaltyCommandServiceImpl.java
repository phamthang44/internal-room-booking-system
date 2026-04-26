package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.utils.PenaltyReasonUtils;
import com.thang.roombooking.common.dto.request.PenaltyExtendRequest;
import com.thang.roombooking.common.dto.request.PenaltyRevokeRequest;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.common.dto.response.UserBasicResponse;
import com.thang.roombooking.common.enums.PenaltyAction;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.PenaltyErrorCode;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.PenaltyRecord;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.PenaltyRecordRepository;
import com.thang.roombooking.service.PenaltyCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PenaltyCommandServiceImpl implements PenaltyCommandService {

    private final PenaltyRecordRepository penaltyRecordRepository;
    private final BookingViolationRepository violationRepository;
    private final PenaltyReasonUtils penaltyReasonUtils;

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

        // Reset the user's violation point slate: soft-delete all violations linked to this
        // penalty so they are invisible to future point calculations. Without this step, the
        // user's accumulated points still count toward the next threshold, causing an immediate
        // re-penalty on their very next violation even after an admin pardon.
        List<BookingViolation> linkedViolations = violationRepository.findByPenalty(penalty);
        if (!linkedViolations.isEmpty()) {
            violationRepository.deleteAll(linkedViolations); // triggers @SQLDelete (soft-delete)
            log.info("{} | Soft-deleted {} violation(s) linked to revoked penalty ID: {} for user: {}",
                    LogConstant.ACTION_SUCCESS, linkedViolations.size(), id, penalty.getUser().getId());
        }

        return getPenaltyRecordResponse(admin, penalty, request.reason(), "penalty.append.revoked_by");
    }

    private PenaltyRecordResponse getPenaltyRecordResponse(UserAccount admin, PenaltyRecord penalty, String reason, String actionKey) {
        // Use structured appending instead of string concatenation to support i18n
        String updatedReason = penaltyReasonUtils.appendReason(penalty.getReason(), actionKey, admin.getEmail());
        
        if (reason != null && !reason.isBlank()) {
            updatedReason = penaltyReasonUtils.appendReason(updatedReason, "penalty.append.reason", reason);
        }
        
        penalty.setReason(updatedReason);

        PenaltyRecord saved = penaltyRecordRepository.save(penalty);

        return PenaltyRecordResponse.builder()
                .id(saved.getId())
                .isActive(saved.isActive())
                .action(saved.getPenaltyAction())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .reason(penaltyReasonUtils.translate(saved.getReason()))
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
}





