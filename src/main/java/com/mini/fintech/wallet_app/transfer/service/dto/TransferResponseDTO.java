package com.mini.fintech.wallet_app.transfer.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;

public record TransferResponseDTO(UUID transferId, UUID sourceWalletId, UUID destinationWalletId, BigDecimal amount,
		String currency, TransferStatus status, String reason, LocalDateTime createdAt, LocalDateTime completedAt) {
}