package com.mini.fintech.wallet_app.outbox.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferReversedEventDTO(UUID reversalId, UUID originalTransferId, UUID sourceWalletId,
		UUID destinationWalletId, BigDecimal amount, String currency, String reason, LocalDateTime occurredAt) {
}
