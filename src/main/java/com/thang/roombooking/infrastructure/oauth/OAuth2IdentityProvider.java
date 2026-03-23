package com.thang.roombooking.infrastructure.oauth;


import com.thang.roombooking.common.enums.IdentityProvider;
import com.thang.roombooking.entity.ExternalIdentity;

public interface OAuth2IdentityProvider {
    IdentityProvider provider();

    ExternalIdentity verify(String idToken);
}
