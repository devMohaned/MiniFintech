package com.mini.fintech.wallet_app.wallet.domain;

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
@Table(name = "wallet_accounts")
public class WalletAccount {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false, length = 100)
	private String customerId;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WalletStatus status;

	@Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
	private BigDecimal availableBalance;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}