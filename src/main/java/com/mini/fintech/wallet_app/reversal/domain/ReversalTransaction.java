package com.mini.fintech.wallet_app.reversal.domain;

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
@Table(name = "reversal_transactions")
public class ReversalTransaction {

	@Id
	private UUID id;

	@Column(name = "original_transfer_id", nullable = false)
	private UUID originalTransferId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReversalStatus status;

	@Column(length = 255)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

}