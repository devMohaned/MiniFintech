package com.mini.fintech.wallet_app.outbox.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

	private final OutboxEventRepository outboxEventRepository;
	private final JsonMapper objectMapper;

	public void saveEvent(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
		log.info(
				"Started to save an outbox event. Aggregate Type [{}], Aggregate ID [{}], Event Type [{}], and Topic [{}].",
				aggregateType, aggregateId, eventType, topic);
		try {
			String payloadJson = objectMapper.writeValueAsString(payload);
			LocalDateTime now = LocalDateTime.now();

			OutboxEvent event = OutboxEvent.builder().id(UUID.randomUUID()).aggregateType(aggregateType)
					.aggregateId(aggregateId).eventType(eventType).topic(topic).payloadJson(payloadJson)
					.status(OutboxStatus.NEW).attempts(0).createdAt(now).nextAttemptAt(now).build();

			outboxEventRepository.save(event);
			log.info(
					"Finished saving outbox event successfully. Outbox Event ID [{}], Event Type [{}], and Topic [{}].",
					event.getId(), event.getEventType(), event.getTopic());
		} catch (Exception ex) {
			log.error(
					"Failed to save outbox event. Aggregate Type [{}], Aggregate ID [{}], Event Type [{}], and Topic [{}].",
					aggregateType, aggregateId, eventType, topic, ex);
			throw ex;
		}
	}
}
