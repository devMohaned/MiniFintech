package com.mini.fintech.wallet_app.outbox.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {

	@Id
	private UUID id;

	@Column(name = "original_event_id", nullable = false, unique = true)
	private UUID originalEventId;

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

	@Column(nullable = false)
	private Integer attempts;

	@Column(name = "last_error_message", columnDefinition = "TEXT")
	private String lastErrorMessage;

	@Column(name = "dead_lettered_at", nullable = false)
	private LocalDateTime deadLetteredAt;
}