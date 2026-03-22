package com.thang.roombooking.infrastructure.idempotency.entity;


import com.thang.roombooking.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "idempotency_keys", schema = "public", indexes = {
        @Index(name = "idx_idempotency_expiry", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey extends BaseAuditEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "key_hash", nullable = false, unique = true, length = 128)
    private String keyHash;

    @Column(name = "resoure_path")
    private String resourcePath;

    @Column(name = "response_code")
    private int responseCode;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "request_fingerprint")
    private String requestFingerprint;

    @Column(name = "expires_at")
    private Instant expiresAt;

}

