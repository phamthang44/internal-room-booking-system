package com.thang.roombooking.service;
 
import com.thang.roombooking.common.dto.request.CreateViolationRequest;
import com.thang.roombooking.common.dto.response.ViolationResponse;
import com.thang.roombooking.entity.UserAccount;
 
public interface ViolationCommandService {
    ViolationResponse createManualViolation(CreateViolationRequest request, UserAccount admin);
}
