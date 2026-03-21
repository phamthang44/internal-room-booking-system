package com.thang.roombooking.infrastructure.idempotency.repository;


import com.thang.roombooking.infrastructure.idempotency.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Integer> {

    Optional<IdempotencyKey> findByKeyHash(String keyHash);

    void deleteByExpiresAtBefore(Instant now);
}
