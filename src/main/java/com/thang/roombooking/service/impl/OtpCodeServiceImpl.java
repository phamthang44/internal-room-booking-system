package com.thang.roombooking.service.impl;



import com.thang.roombooking.common.exception.OtpCodeException;
import com.thang.roombooking.repository.OtpCodeRepository;
import com.thang.roombooking.service.OtpCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpCodeServiceImpl implements OtpCodeService {

    private final OtpCodeRepository otpCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final long OTP_TTL_MINUTES = 5;

    @Override
    public String generateAndSaveOtp(String email) {
        String otpCode = String.valueOf(secureRandom.nextInt(900000) + 100000);
        otpCodeRepository.save(email, otpCode, OTP_TTL_MINUTES);
        log.info("Generated OTP for email: {}", email);
        return otpCode;
    }

    @Override
    public boolean verifyOtpCode(String email, String code) {
        String storedOtp = otpCodeRepository.findByEmail(email);

        if (storedOtp == null) {
            throw new OtpCodeException();
        }
        if (!storedOtp.equals(code)) {
            throw new OtpCodeException();
        }

        otpCodeRepository.deleteByEmail(email);

        return true;
    }
}
