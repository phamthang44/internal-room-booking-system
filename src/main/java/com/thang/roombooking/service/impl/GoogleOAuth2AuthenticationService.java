// src/main/java/com/thang/aurea/auth/internal/application/service/GoogleOAuth2AuthenticationService.java
package com.thang.roombooking.service.impl;



import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.entity.ExternalIdentity;
import com.thang.roombooking.infrastructure.oauth.OAuth2IdentityProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for Google OAuth2 authentication.
 * Handles the flow: verify identity ->
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2AuthenticationService {

    private final OAuth2IdentityProvider identityProvider; // GoogleIdentityProvider

    public ExternalIdentity getUserInfo(String idToken) {
        log.info("{} | Verifying Google ID token.", LogConstant.ACTION_START);
        try {
            // 1. Xác thực ID Token trực tiếp
            ExternalIdentity identity = identityProvider.verify(idToken);
            log.info("{} | Successfully verified Google identity. Email: {}", LogConstant.ACTION_SUCCESS, identity.email());
            return identity;
        } catch (Exception e) {
            log.error("{} | Failed to verify Google ID token: ", LogConstant.SYS_ERROR, e);
            throw e;
        }
        // HẾT. Không Save DB, Không tạo Token ở đây.
    }
}
