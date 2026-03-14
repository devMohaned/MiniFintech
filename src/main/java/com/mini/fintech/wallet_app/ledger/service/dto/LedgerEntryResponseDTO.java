package com.mini.fintech.wallet_app.ledger.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;

public record LedgerEntryResponseDTO(UUID id, UUID transactionId, UUID walletId, LedgerEntryType entryType,
		BigDecimal amount, String currency, LedgerReferenceType referenceType, String referenceId, String description,
		LocalDateTime createdAt) {
}
