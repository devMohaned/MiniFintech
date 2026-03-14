package com.mini.fintech.wallet_app.idemptotency.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyRecord;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

	Optional<IdempotencyRecord> findByIdempotencyKeyAndOperationType(String idempotencyKey, String operationType);
}