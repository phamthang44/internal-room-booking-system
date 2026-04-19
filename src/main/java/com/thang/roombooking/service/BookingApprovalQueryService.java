package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.ApprovalSearchRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.ApprovalSummaryResponse;
import com.thang.roombooking.entity.UserAccount;

import java.util.List;

public interface BookingApprovalQueryService {

    /**
     * Paginated search for booking approvals (approved/rejected history).
     * Scoped to the current authenticated user.
     *
     * @param request search filters and pagination params
     * @param currentUser authenticated user context
     * @return a wrapped list of approval summaries with pagination metadata
     */
    ApiResult<List<ApprovalSummaryResponse>> searchApprovals(ApprovalSearchRequest request, UserAccount currentUser);

}
