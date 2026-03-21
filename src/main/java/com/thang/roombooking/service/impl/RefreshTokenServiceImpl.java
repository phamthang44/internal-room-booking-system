package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.exception.TokenRefreshException;
import com.thang.roombooking.entity.RefreshToken;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.RefreshTokenRepository;
import com.thang.roombooking.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration:2592000000}")
    private Long refreshTokenDurationMs;

    @Override
    @Transactional
    public RefreshToken saveRefreshToken(UserAccount user, String tokenString) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new TokenRefreshException(rawToken, I18nUtils.get("error.token_invalid")));

        if (token.isRevoked()) {
            log.warn("Attempt to use revoked refresh token: {}", rawToken);
            throw new TokenRefreshException(rawToken, I18nUtils.get("error.token_revoked"));
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(rawToken, I18nUtils.get("error.token_expired"));
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user: {}", token.getUser().getEmail());
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(UserAccount user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.info("All refresh tokens revoked for user: {}", user.getEmail());
    }
}
