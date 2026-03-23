package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.AuthResponse;
import com.thang.roombooking.entity.ExternalIdentity;

public interface OAuthService {
    AuthResponse loginWithGoogle(ExternalIdentity externalIdentity);
}
