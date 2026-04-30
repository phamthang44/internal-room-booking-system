package com.thang.roombooking.service;



public interface OtpCodeService {

    String generateAndSaveOtp(String email);
    boolean verifyOtpCode(String email, String code);

}
