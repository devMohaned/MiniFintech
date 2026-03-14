package com.mini.fintech.wallet_app.ledger.domain;

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
@Table(name = "ledger_entries")
public class LedgerEntry {

	@Id
	private UUID id;

	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@Column(name = "wallet_id", nullable = false)
	private UUID walletId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 10)
	private LedgerEntryType entryType;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type", nullable = false, length = 30)
	private LedgerReferenceType referenceType;

	@Column(name = "reference_id", nullable = false, length = 100)
	private String referenceId;

	@Column(length = 255)
	private String description;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
