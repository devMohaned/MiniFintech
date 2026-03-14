package com.mini.fintech.wallet_app.reversal.service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.mini.fintech.wallet_app.reversal.domain.ReversalStatus;

public record ReversalResponseDTO(UUID reversalId, UUID originalTransferId, ReversalStatus status, String reason,
		LocalDateTime createdAt, LocalDateTime completedAt) {
}