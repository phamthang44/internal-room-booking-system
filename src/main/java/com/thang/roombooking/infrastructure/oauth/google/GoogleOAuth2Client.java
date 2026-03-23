package com.thang.roombooking.infrastructure.oauth.google;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2Client {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    /**
     * Xác thực Google ID Token gửi từ Frontend.
     * Phương pháp này verify chữ ký số (local validation), cực kỳ nhanh và bảo mật.
     */
    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        log.info("Verifying Google ID Token using local SDK verifier");

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    // Quan trọng: Kiểm tra Token này có phải cấp cho App của mình không
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                // Log để debug (chỉ log email, không log token)
                log.info("Successfully verified Google Token for email: {}", payload.getEmail());

                return payload;
            } else {
                log.warn("Invalid Google ID Token provided");
                throw new AppException(CommonErrorCode.OAUTH_ERROR, "Invalid or expired Google Token");
            }

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Critical error during Google Token verification: ", e);
            throw new AppException(CommonErrorCode.INTERNAL_ERROR, "Error verifying Google Token: " + e.getMessage());
        }
    }

    public Map<String, Object> verifyIdToken(String idToken) {
        log.info("Verifying Google ID token");
        try {
            Map<String, Object> userInfo = webClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                            .doOnNext(errorBody -> log.error("Google ID token verification failed. Status: {}, Response: {}", clientResponse.statusCode(), errorBody))
                            .then(Mono.error(new AppException(CommonErrorCode.OAUTH_ERROR, "Invalid or expired Google ID token")));
                    })
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
                    
            if (userInfo == null) {
                throw new AppException(CommonErrorCode.OAUTH_ERROR, "Failed to retrieve user info from Google");
            }

            String aud = (String) userInfo.get("aud");
            if (!clientId.equals(aud)) {
                log.error("Token audience {} doesn't match client ID {}", aud, clientId);
                throw new AppException(CommonErrorCode.OAUTH_ERROR, "Invalid ID token audience");
            }

            log.info("Successfully verified ID token. Email: {}", userInfo.get("email"));
            return userInfo;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying ID token from Google: ", e);
            throw new AppException(CommonErrorCode.OAUTH_ERROR, "Failed to verify ID token: " + e.getMessage());
        }
    }
}
