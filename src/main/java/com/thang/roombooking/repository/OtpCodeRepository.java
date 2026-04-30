package com.thang.roombooking.repository;


public interface OtpCodeRepository {
    void save(String email, String otpCode, long ttlMinutes);
    String findByEmail(String email);
    void deleteByEmail(String email);
}
