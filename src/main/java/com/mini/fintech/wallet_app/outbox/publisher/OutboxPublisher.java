package com.mini.fintech.wallet_app.outbox.publisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

	private static final Set<OutboxStatus> RETRIABLE_STATUSES = Set.of(OutboxStatus.NEW, OutboxStatus.FAILED);

	private final OutboxEventRepository outboxEventRepository;
	private final OutboxPublishWorker outboxPublishWorker;

	@Value("${wallet.outbox.publisher.batch-size}")
	private int batchSize;

	@Scheduled(fixedDelayString = "${wallet.outbox.publisher.fixed-delay-ms}")
	@Transactional
	public void publishPendingEvents() {
		log.info("Started scheduled outbox publishing cycle with batch size [{}].", batchSize);
		try {
			List<OutboxEvent> events = outboxEventRepository
					.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(RETRIABLE_STATUSES,
							LocalDateTime.now(), PageRequest.of(0, batchSize));

			if (events.isEmpty()) {
				log.debug("No eligible outbox events were found for publishing in this cycle.");
				return;
			}

			log.info("Fetched [{}] eligible outbox events for publishing in this cycle.", events.size());
			for (OutboxEvent event : events) {
				try {
					log.info(
							"Started dispatching outbox event from scheduler. Event ID [{}], Event Type [{}], and Topic [{}].",
							event.getId(), event.getEventType(), event.getTopic());
					outboxPublishWorker.publishSingle(event.getId());
					log.info("Finished dispatching outbox event from scheduler. Event ID [{}].", event.getId());
				} catch (Exception ex) {
					log.error("Unexpected scheduler error while dispatching outbox event. Event ID [{}].",
							event.getId(), ex);
				}
			}

			log.info("Finished scheduled outbox publishing cycle.");
		} catch (Exception ex) {
			log.error("Failed scheduled outbox publishing cycle.", ex);
			throw ex;
		}
	}
}
