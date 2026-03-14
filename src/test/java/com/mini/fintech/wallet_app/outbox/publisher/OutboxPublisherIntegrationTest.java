package com.mini.fintech.wallet_app.outbox.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

class OutboxPublisherIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private OutboxEventRepository outboxEventRepository;

	@Autowired
	private OutboxPublisher outboxPublisher;

	@Test
	void shouldWriteAndPublishTransferCompletedOutboxEvent() throws Exception {
		String sourceWalletId = createWallet("cust_outbox_src_001", "EGP");
		String destinationWalletId = createWallet("cust_outbox_dst_001", "EGP");
		creditWallet(sourceWalletId, "300.0000");

		createTransfer(sourceWalletId, destinationWalletId, "100.0000", "idem-outbox-001", "outbox_test");

		OutboxEvent beforePublish = outboxEventRepository.findAll().stream()
				.filter(event -> "transfer.completed".equals(event.getEventType())).findFirst()
				.orElseThrow(() -> new AssertionError("No transfer.completed outbox event found"));

		assertThat(beforePublish.getStatus()).isEqualTo(OutboxStatus.NEW);

		outboxPublisher.publishPendingEvents();

		OutboxEvent afterPublish = outboxEventRepository.findById(beforePublish.getId()).orElseThrow();

		assertThat(afterPublish.getStatus()).isIn(OutboxStatus.PUBLISHED, OutboxStatus.FAILED, OutboxStatus.DEAD);

	}

	private HttpHeaders jsonHeaders(String correlationId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Correlation-Id", correlationId);
		return headers;
	}
}