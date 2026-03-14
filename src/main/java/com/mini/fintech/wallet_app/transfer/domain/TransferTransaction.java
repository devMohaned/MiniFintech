package com.mini.fintech.wallet_app.transfer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transfer_transactions")
public class TransferTransaction {

	@Id
	private UUID id;

	@Column(name = "source_wallet_id", nullable = false)
	private UUID sourceWalletId;

	@Column(name = "destination_wallet_id", nullable = false)
	private UUID destinationWalletId;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferStatus status;

	@Column(length = 255)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "reversed_at")
	private LocalDateTime reversedAt;
}