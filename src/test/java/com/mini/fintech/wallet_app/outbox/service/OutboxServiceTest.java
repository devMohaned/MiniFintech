package com.mini.fintech.wallet_app.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;
import com.mini.fintech.wallet_app.outbox.repo.OutboxEventRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {
	@Mock
	private OutboxEventRepository outboxEventRepository;

	@Mock
	private JsonMapper jsonMapper;

	@InjectMocks
	private OutboxService outboxService;

	@Test
	void shouldSaveOutboxEventSuccessfully() throws Exception {
		Mockito.doReturn("{\"ok\":true}").when(jsonMapper).writeValueAsString(any());

		outboxService.saveEvent("TRANSFER", "aggregate-001", "transfer.completed", "fintech.transfer.completed",
				java.util.Map.of("ok", true));

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(captor.capture());

		OutboxEvent saved = captor.getValue();

		assertThat(saved.getAggregateType()).isEqualTo("TRANSFER");
		assertThat(saved.getAggregateId()).isEqualTo("aggregate-001");
		assertThat(saved.getEventType()).isEqualTo("transfer.completed");
		assertThat(saved.getTopic()).isEqualTo("fintech.transfer.completed");
		assertThat(saved.getPayloadJson()).isEqualTo("{\"ok\":true}");
		assertThat(saved.getStatus()).isEqualTo(OutboxStatus.NEW);
		assertThat(saved.getAttempts()).isEqualTo(0);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getNextAttemptAt()).isNotNull();
	}

	@Test
	void shouldThrowIllegalStateExceptionWhenSerializationFails() throws Exception {
		Mockito.doThrow(new RuntimeException("Boom")).when(jsonMapper).writeValueAsString(any());

		assertThatThrownBy(() -> outboxService.saveEvent("TRANSFER", "aggregate-001", "transfer.completed",
				"fintech.transfer.completed", Map.of("bad", true))).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Boom");

		verify(outboxEventRepository, never()).save(any());
	}
}
