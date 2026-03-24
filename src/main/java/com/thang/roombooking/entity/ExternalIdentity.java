package com.thang.roombooking.entity;


import com.thang.roombooking.common.enums.IdentityProvider;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.CommonErrorCode;
import lombok.Builder;


import java.util.Map;

@Builder
public record ExternalIdentity(
        IdentityProvider provider,
        String externalUserId,
        String email,
        String name,
        String avatarUrl
) {
    public ExternalIdentity {
        if (provider == null) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Provider cannot be null");
        }
        if (externalUserId == null || externalUserId.isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "External user ID cannot be null or blank");
        }
        if (email == null || email.isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Email cannot be null or blank");
        }
    }

    /**
     * Factory method to create from Google OAuth2 attributes
     * You can add more providers later (Facebook, GitHub, etc.)
     */
    public static ExternalIdentity fromGoogleAttributes(Map<String, Object> attributes) {
        return new ExternalIdentity(
                IdentityProvider.GOOGLE,
                (String) attributes.get("sub"), // Google's user ID field
                (String) attributes.get("email"),
                (String) attributes.get("name"),
                (String) attributes.get("picture")
        );
    }
}
