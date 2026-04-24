package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.PenaltyExtendRequest;
import com.thang.roombooking.common.dto.request.PenaltyRevokeRequest;
import com.thang.roombooking.common.dto.response.PenaltyRecordResponse;
import com.thang.roombooking.entity.UserAccount;

public interface PenaltyCommandService {

    PenaltyRecordResponse revokePenalty(Long id, PenaltyRevokeRequest request, UserAccount admin);
    PenaltyRecordResponse extendPenalty(Long id, PenaltyExtendRequest request, UserAccount admin);

}
