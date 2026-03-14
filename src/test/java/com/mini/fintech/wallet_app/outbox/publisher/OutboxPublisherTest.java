package com.mini.fintech.wallet_app.outbox.publisher;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class OutboxPublisherTest {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private OutboxPublishWorker outboxPublishWorker;

	@InjectMocks
	private OutboxPublisher outboxPublisher;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(outboxPublisher, "batchSize", 50);
	}

	@Test
	void shouldPublishAllReadyEvents() {
		OutboxEvent e1 = event(UUID.randomUUID());
		OutboxEvent e2 = event(UUID.randomUUID());

		when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(anyCollection(),
				any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(e1, e2));

		outboxPublisher.publishPendingEvents();

		verify(outboxPublishWorker).publishSingle(e1.getId());
		verify(outboxPublishWorker).publishSingle(e2.getId());
	}

	@Test
	void shouldNotFailWholeCycleWhenOnePublishThrowsUnexpectedly() {
		OutboxEvent e1 = event(UUID.randomUUID());
		OutboxEvent e2 = event(UUID.randomUUID());

		when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(anyCollection(),
				any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(e1, e2));

		doThrow(new RuntimeException("boom")).when(outboxPublishWorker).publishSingle(e1.getId());

		outboxPublisher.publishPendingEvents();

		verify(outboxPublishWorker).publishSingle(e1.getId());
		verify(outboxPublishWorker).publishSingle(e2.getId());
	}

	@Test
	void shouldRethrowWhenScheduledCycleFailsBeforeProcessingAnyEvent() {
		when(outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(anyCollection(),
				any(LocalDateTime.class), any(Pageable.class))).thenThrow(new RuntimeException("db boom"));

		assertThatThrownBy(() -> outboxPublisher.publishPendingEvents()).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("db boom");

		verifyNoInteractions(outboxPublishWorker);
	}

	private OutboxEvent event(UUID id) {
		return OutboxEvent.builder().id(id).aggregateType("TRANSFER").aggregateId("agg-" + id)
				.eventType("transfer.completed").topic("fintech.transfer.completed").payloadJson("{}")
				.status(OutboxStatus.NEW).attempts(0).createdAt(LocalDateTime.now()).nextAttemptAt(LocalDateTime.now())
				.build();
	}
}