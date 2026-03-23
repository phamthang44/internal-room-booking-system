package com.thang.roombooking.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.thang.roombooking.common.exception.TokenErrorException;
import com.thang.roombooking.common.exception.TokenExpiredException;
import com.thang.roombooking.common.utils.KeyUtils;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.infrastructure.security.SecurityUserDetails;
import com.thang.roombooking.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final KeyUtils keyUtils;

    @Override
    public String generateAccessToken(UserAccount userAccount) {
        RSAKey rsaKey = keyUtils.getRsaKey();
        Instant now = Instant.now();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userAccount.getUsername())
                .claim("userId", userAccount.getId())
                .claim("role", userAccount.getRole().getName())
                .issuer("room-booking-service")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();

        return signJwt(rsaKey, header, claims);
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String extractUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSAKey rsaKey = keyUtils.getRsaKey();
            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

            if (!signedJWT.verify(verifier)) {
                throw new TokenErrorException(I18nUtils.get("error.token_invalid"));
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                throw new TokenExpiredException(I18nUtils.get("error.token_expired"));
            }

            return claims.getSubject();
        } catch (TokenExpiredException | TokenErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenErrorException(I18nUtils.get("error.unexpected_error_occurred"));
        }
    }

    @Override
    public boolean isValid(String token, SecurityUserDetails userDetails) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSAKey rsaKey = keyUtils.getRsaKey();
            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

            if (!signedJWT.verify(verifier)) {
                return false;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            String username = claims.getSubject();

            boolean isUsernameMatch = username.equals(userDetails.getUsername());
            boolean isNotExpired = expirationTime != null && expirationTime.after(new Date());

            return isUsernameMatch && isNotExpired;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getRemainingTimeInSeconds(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            RSAKey rsaKey = keyUtils.getRsaKey();
            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

            if (!signedJWT.verify(verifier)) {
                return 0;
            }

            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            long diff = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, diff / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    private String signJwt(RSAKey rsaKey, JWSHeader header, JWTClaimsSet claims) {
        SignedJWT signedJWT = new SignedJWT(header, claims);
        try {
            JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new TokenErrorException(I18nUtils.get("error.unexpected_error_occurred"));
        }
        return signedJWT.serialize();
    }
}
