package com.thang.roombooking.infrastructure.idempotency.entity;


import com.thang.roombooking.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "idempotency_keys", schema = "public")
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

    @Column(name = "reference", length = 128)
    private String reference;

    @Column(name = "request_fingerprint")
    private String requestFingerprint;

    @Column(name = "expires_at")
    private Instant expiresAt;

}

