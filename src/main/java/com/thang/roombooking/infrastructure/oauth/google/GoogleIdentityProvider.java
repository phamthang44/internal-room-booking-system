package com.thang.roombooking.infrastructure.oauth.google;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.thang.roombooking.common.enums.IdentityProvider;
import com.thang.roombooking.entity.ExternalIdentity;
import com.thang.roombooking.infrastructure.oauth.OAuth2IdentityProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Infrastructure: Only handles Google API communication.
 * No domain logic here - just convert Google response to ExternalIdentity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleIdentityProvider implements OAuth2IdentityProvider {

    private final GoogleOAuth2Client googleOAuth2Client;

    @Override
    public IdentityProvider provider() {
        return IdentityProvider.GOOGLE;
    }

    @Override
    public ExternalIdentity verify(String idToken) {
        // 1. Dùng Client để verify ID Token và lấy thông tin user
//        Map<String, Object> attributes = googleOAuth2Client.verifyIdToken(idToken);
        GoogleIdToken.Payload payload = googleOAuth2Client.verifyToken(idToken);
        // 2. Convert map của Google thành ExternalIdentity của hệ thống mình
        return ExternalIdentity.builder()
                .provider(IdentityProvider.GOOGLE)
                .externalUserId((String) payload.get("sub")) // Google ID Token trả về sub
                .email((String) payload.get("email"))
                .name((String) payload.get("name"))
                .avatarUrl((String) payload.get("picture"))
                .build();
    }
}
