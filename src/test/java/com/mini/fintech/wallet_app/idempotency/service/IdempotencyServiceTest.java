package com.mini.fintech.wallet_app.idempotency.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyRecord;
import com.mini.fintech.wallet_app.idemptotency.repo.IdempotencyRecordRepository;
import com.mini.fintech.wallet_app.idemptotency.service.IdempotencyService;
import com.mini.fintech.wallet_app.transfer.service.dto.CreateTransferRequestDTO;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest extends BaseIntegrationTest {

	@MockitoSpyBean
	private IdempotencyService idempotencyService;

	@MockitoBean
	private IdempotencyRecordRepository idempotencyRecordRepository;

	@Test
	void shouldThrowIllegalStateExceptionWhenHashAlgorithmIsInvalid() {
		ReflectionTestUtils.setField(idempotencyService, "hashingAlgorithm", "NOT_A_REAL_ALGO");

		assertThatThrownBy(() -> idempotencyService.hashTransferRequest(buildDummyModel()))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_A_REAL_ALGO");
	}

	@Test
	void shouldThrowIllegalStateExceptionWhenEncodingIsInvalid() {
		ReflectionTestUtils.setField(idempotencyService, "encoding", "NOT_A_REAL_ENCODING");

		assertThatThrownBy(() -> idempotencyService.hashTransferRequest(buildDummyModel()))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Encoding");
	}

	private CreateTransferRequestDTO buildDummyModel() {

		CreateTransferRequestDTO createTransferRequestDTO = new CreateTransferRequestDTO();
		createTransferRequestDTO.setAmount(BigDecimal.ONE);
		createTransferRequestDTO.setReason("");
		createTransferRequestDTO.setCurrency("");
		createTransferRequestDTO.setDestinationWalletId(UUID.randomUUID());
		createTransferRequestDTO.setSourceWalletId(UUID.randomUUID());
		return createTransferRequestDTO;
	}

	@Test
	void shouldThrowBusinessExceptionWhenDuplicateProcessingRecordIsCreated() {

		Mockito.doThrow(new DataIntegrityViolationException("duplicate key")).when(idempotencyRecordRepository)
				.save(Mockito.any(IdempotencyRecord.class));

		assertThatThrownBy(() -> idempotencyService.createProcessingRecord("idem-001", "TRANSFER", "request-hash-001"))
				.isInstanceOf(BusinessException.class).hasMessageContaining("already being processed");
	}

	@Test
	void shouldRethrowWhenMarkCompletedSaveFails() {
		IdempotencyRecord record = IdempotencyRecord.builder().id(UUID.randomUUID()).idempotencyKey("key")
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
		Mockito.doThrow(new RuntimeException("db failure")).when(idempotencyRecordRepository)
				.save(Mockito.notNull(IdempotencyRecord.class));

		assertThatThrownBy(() -> idempotencyService.markCompleted(record, UUID.randomUUID().toString(), 200, ""))
				.isInstanceOf(RuntimeException.class).hasMessageContaining("db failure");
	}
}