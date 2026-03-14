package com.mini.fintech.wallet_app.outbox.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini.fintech.wallet_app.outbox.domain.DeadLetterEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.DeadLetterEventRepository;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxPublishWorkerTest {

	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private DeadLetterEventRepository deadLetterEventRepository;

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@InjectMocks
	private OutboxPublishWorker outboxPublishWorker;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(outboxPublishWorker, "maxAttempts", 3);
		ReflectionTestUtils.setField(outboxPublishWorker, "initialDelayMs", 2000L);
		ReflectionTestUtils.setField(outboxPublishWorker, "multiplier", 2.0d);
		ReflectionTestUtils.setField(outboxPublishWorker, "maxDelayMs", 60000L);
	}

	@Test
	void shouldMarkEventPublishedWhenKafkaSendSucceeds() throws Exception {
		UUID eventId = UUID.randomUUID();
		OutboxEvent event = buildEvent(eventId, OutboxStatus.NEW, 0);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn((CompletableFuture) future);

		outboxPublishWorker.publishSingle(eventId);

		ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository, atLeastOnce()).save(eventCaptor.capture());

		OutboxEvent saved = eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1);

		assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
		assertThat(saved.getAttempts()).isEqualTo(1);
		assertThat(saved.getPublishedAt()).isNotNull();
		assertThat(saved.getErrorMessage()).isNull();
	}

	@Test
	void shouldMarkEventFailedAndScheduleRetryWhenKafkaSendFailsBeforeMaxAttempts() throws Exception {
		UUID eventId = UUID.randomUUID();
		OutboxEvent event = buildEvent(eventId, OutboxStatus.NEW, 0);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		CompletableFuture<Object> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn((CompletableFuture) failedFuture);

		outboxPublishWorker.publishSingle(eventId);

		ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository, atLeastOnce()).save(eventCaptor.capture());

		OutboxEvent saved = eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1);

		assertThat(saved.getStatus()).isEqualTo(OutboxStatus.FAILED);
		assertThat(saved.getAttempts()).isEqualTo(1);
		assertThat(saved.getErrorMessage()).contains("Kafka down");
		assertThat(saved.getNextAttemptAt()).isAfter(LocalDateTime.now().minusSeconds(1));

		verify(deadLetterEventRepository, never()).save(any());
	}

	@Test
	void shouldMoveEventToDeadLetterWhenMaxAttemptsReached() throws Exception {
		UUID eventId = UUID.randomUUID();
		OutboxEvent event = buildEvent(eventId, OutboxStatus.FAILED, 2);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		CompletableFuture<Object> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new RuntimeException("Poison event"));
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn((CompletableFuture) failedFuture);

		outboxPublishWorker.publishSingle(eventId);

		ArgumentCaptor<DeadLetterEvent> deadLetterCaptor = ArgumentCaptor.forClass(DeadLetterEvent.class);
		verify(deadLetterEventRepository).save(deadLetterCaptor.capture());

		DeadLetterEvent deadLetterEvent = deadLetterCaptor.getValue();
		assertThat(deadLetterEvent.getOriginalEventId()).isEqualTo(eventId);
		assertThat(deadLetterEvent.getAttempts()).isEqualTo(3);
		assertThat(deadLetterEvent.getLastErrorMessage()).contains("Poison event");

		ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository, atLeastOnce()).save(eventCaptor.capture());

		OutboxEvent saved = eventCaptor.getAllValues().get(eventCaptor.getAllValues().size() - 1);
		assertThat(saved.getStatus()).isEqualTo(OutboxStatus.DEAD);
		assertThat(saved.getAttempts()).isEqualTo(3);
	}

	@Test
	void shouldDoNothingWhenEventDoesNotExist() {
		UUID eventId = UUID.randomUUID();
		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.empty());

		outboxPublishWorker.publishSingle(eventId);

		verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
		verify(outboxEventRepository, never()).save(any());
		verify(deadLetterEventRepository, never()).save(any());
	}

	@Test
	void shouldDoNothingWhenEventAlreadyPublished() {
		UUID eventId = UUID.randomUUID();
		OutboxEvent event = buildEvent(eventId, OutboxStatus.PUBLISHED, 1);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		outboxPublishWorker.publishSingle(eventId);

		verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
		verify(outboxEventRepository, never()).save(any());
		verify(deadLetterEventRepository, never()).save(any());
	}

	@Test
	void shouldDoNothingWhenEventAlreadyDead() {
		UUID eventId = UUID.randomUUID();
		OutboxEvent event = buildEvent(eventId, OutboxStatus.DEAD, 3);

		when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));

		outboxPublishWorker.publishSingle(eventId);

		verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
		verify(outboxEventRepository, never()).save(any());
		verify(deadLetterEventRepository, never()).save(any());
	}

	private OutboxEvent buildEvent(UUID id, OutboxStatus status, int attempts) {
		return OutboxEvent.builder().id(id).aggregateType("TRANSFER").aggregateId(UUID.randomUUID().toString())
				.eventType("transfer.completed").topic("fintech.transfer.completed").payloadJson("{\"test\":true}")
				.status(status).attempts(attempts).createdAt(LocalDateTime.now()).nextAttemptAt(LocalDateTime.now())
				.build();
	}
}