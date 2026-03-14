package com.mini.fintech.wallet_app.common.dto;

import java.time.LocalDateTime;

public record ApiErrorDTO(String code, String message, String correlationId, LocalDateTime timestamp) {
}
