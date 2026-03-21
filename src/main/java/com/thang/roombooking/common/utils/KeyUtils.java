package com.thang.roombooking.common.utils;

import com.nimbusds.jose.jwk.RSAKey;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class KeyUtils {

    @Value("${jwt.key.private-path}")
    private String privateKeyPath;

    @Value("${jwt.key.public-path}")
    private String publicKeyPath;

    @Value("${jwt.key.id}")
    private String keyId;

    public RSAKey getRsaKey() {
        try {
            String privateKeyContent = readKeyFile(privateKeyPath)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

            String publicKeyContent = readKeyFile(publicKeyPath)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.INTERNAL_ERROR, "Failed to load RSA key files: " + e.getMessage());
        }
    }

    private String readKeyFile(String path) throws IOException {
        return new String(new ClassPathResource(path.replace("classpath:", "")).getInputStream().readAllBytes());
    }
}