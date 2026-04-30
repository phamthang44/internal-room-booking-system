package com.thang.roombooking.common.exception;

import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;

public class OtpCodeException extends AppException {
    public OtpCodeException() {
        super(AuthErrorCode.OTP_INVALID);
    }
}
