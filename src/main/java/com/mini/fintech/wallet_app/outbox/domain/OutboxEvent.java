package com.mini.fintech.wallet_app.outbox.domain;

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
@Table(name = "outbox_events")
public class OutboxEvent {

	@Id
	private UUID id;

	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@Column(name = "aggregate_id", nullable = false, length = 100)
	private String aggregateId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(nullable = false, length = 255)
	private String topic;

	@Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
	private String payloadJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OutboxStatus status;

	@Column(nullable = false)
	private Integer attempts;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "next_attempt_at", nullable = false)
	private LocalDateTime nextAttemptAt;
}