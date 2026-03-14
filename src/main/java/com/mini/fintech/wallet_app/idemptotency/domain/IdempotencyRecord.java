package com.mini.fintech.wallet_app.idemptotency.domain;

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
@Table(name = "idempotency_records")
public class IdempotencyRecord {

	@Id
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, length = 255)
	private String idempotencyKey;

	@Column(name = "operation_type", nullable = false, length = 50)
	private String operationType;

	@Column(name = "request_hash", nullable = false, length = 128)
	private String requestHash;

	@Column(name = "resource_id", length = 100)
	private String resourceId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IdempotencyStatus status;

	@Column(name = "response_code")
	private Integer responseCode;

	@Column(name = "response_body", columnDefinition = "TEXT")
	private String responseBody;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}