package com.mini.fintech.wallet_app.wallet.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;

public record WalletResponseDTO(UUID id, String customerId, String currency, WalletStatus status,
		BigDecimal availableBalance, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
