package com.mini.fintech.wallet_app.outbox.publisher;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mini.fintech.wallet_app.outbox.domain.DeadLetterEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.DeadLetterEventRepository;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublishWorker {

	private final OutboxEventRepository outboxEventRepository;
	private final DeadLetterEventRepository deadLetterEventRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Value("${wallet.outbox.publisher.max-attempts}")
	private int maxAttempts;

	@Value("${wallet.outbox.publisher.backoff.initial-delay-ms}")
	private long initialDelayMs;

	@Value("${wallet.outbox.publisher.backoff.multiplier}")
	private double multiplier;

	@Value("${wallet.outbox.publisher.backoff.max-delay-ms}")
	private long maxDelayMs;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void publishSingle(UUID eventId) {
		log.info("Started publishing a single outbox event. Event ID [{}].", eventId);
		OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);

		if (event == null) {
			log.warn("Skipping outbox publish because event was not found. Event ID [{}].", eventId);
			return;
		}

		if (OutboxStatus.PUBLISHED.equals(event.getStatus()) || OutboxStatus.DEAD.equals(event.getStatus())) {
			log.info(
					"Skipping outbox publish because event status is not publishable. Event ID [{}] and Current Status [{}].",
					event.getId(), event.getStatus());
			return;
		}

		try {
			kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayloadJson()).get();
			event.setStatus(OutboxStatus.PUBLISHED);
			event.setPublishedAt(LocalDateTime.now());
			event.setErrorMessage(null);
			event.setAttempts(event.getAttempts() + 1);
			outboxEventRepository.save(event);
			log.info("Finished publishing outbox event successfully. Event ID [{}], Topic [{}], and Attempts [{}].",
					event.getId(), event.getTopic(), event.getAttempts());
		} catch (Exception ex) {
			log.error("Failed to publish outbox event. Event ID [{}], Topic [{}], and Current Attempts [{}].",
					event.getId(), event.getTopic(), event.getAttempts(), ex);

			int newAttempts = event.getAttempts() + 1;
			event.setAttempts(newAttempts);
			event.setErrorMessage(ex.getMessage());

			if (newAttempts >= maxAttempts) {
				deadLetterEventRepository.save(DeadLetterEvent.builder().id(UUID.randomUUID())
						.originalEventId(event.getId()).aggregateType(event.getAggregateType())
						.aggregateId(event.getAggregateId()).eventType(event.getEventType()).topic(event.getTopic())
						.payloadJson(event.getPayloadJson()).attempts(newAttempts).lastErrorMessage(ex.getMessage())
						.deadLetteredAt(LocalDateTime.now()).build());

				event.setStatus(OutboxStatus.DEAD);
				event.setNextAttemptAt(LocalDateTime.now());
				log.warn(
						"Moved outbox event to dead-letter after reaching maximum attempts. Event ID [{}], Attempts [{}], and Max Attempts [{}].",
						event.getId(), newAttempts, maxAttempts);
			} else {
				long backoffDelayMs = computeBackoffDelayMs(newAttempts);
				event.setStatus(OutboxStatus.FAILED);
				event.setNextAttemptAt(LocalDateTime.now().plusNanos(backoffDelayMs * 1_000_000));
				log.warn(
						"Scheduled outbox event for retry after publish failure. Event ID [{}], Attempts [{}], and Backoff Delay Ms [{}].",
						event.getId(), newAttempts, backoffDelayMs);
			}

			outboxEventRepository.save(event);
		}
	}

	/*
	 * Failure #1 -> retry after 2s Failure #2 -> retry after 4s Failure #3 -> retry
	 * after 8s Failure #4 -> retry after 16s Failure #5 -> retry after 32s
	 */
	private long computeBackoffDelayMs(int attempts) {
		double delay = initialDelayMs * Math.pow(multiplier, Math.max(0, attempts - 1));
		long computedDelay = Math.min((long) delay, maxDelayMs);
		log.debug("Computed outbox backoff delay. Attempts [{}], Raw Delay [{}], and Capped Delay [{}].", attempts,
				(long) delay, computedDelay);
		return computedDelay;
	}
}
