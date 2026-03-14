package com.mini.fintech.wallet_app.reversal.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini.fintech.wallet_app.reversal.domain.ReversalTransaction;

public interface ReversalTransactionRepository extends JpaRepository<ReversalTransaction, UUID> {

	Optional<ReversalTransaction> findByOriginalTransferId(UUID originalTransferId);

	boolean existsByOriginalTransferId(UUID originalTransferId);
}
