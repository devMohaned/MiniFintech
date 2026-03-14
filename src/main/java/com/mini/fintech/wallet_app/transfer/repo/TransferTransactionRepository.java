package com.mini.fintech.wallet_app.transfer.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;

public interface TransferTransactionRepository extends JpaRepository<TransferTransaction, UUID> {
}