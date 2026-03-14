package com.mini.fintech.wallet_app.outbox.service.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.mini.fintech.wallet_app.outbox.service.dto.TransferCompletedEventDTO;
import com.mini.fintech.wallet_app.outbox.service.dto.TransferReversedEventDTO;
import com.mini.fintech.wallet_app.reversal.domain.ReversalStatus;
import com.mini.fintech.wallet_app.reversal.domain.ReversalTransaction;
import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;

class OutboxEventMapperTest {

	private final OutboxEventMapper outboxEventMapper = Mappers.getMapper(OutboxEventMapper.class);

	@Test
	void toTransferCompletedEvent_whenSourceIsNull_shouldReturnNull() {
		TransferCompletedEventDTO response = outboxEventMapper.toTransferCompletedEvent(null);

		assertNull(response);
	}

	@Test
	void toTransferReversedEvent_whenBothSourcesAreNull_shouldReturnNull() {
		TransferReversedEventDTO response = outboxEventMapper.toTransferReversedEvent(null, null);

		assertNull(response);
	}

	@Test
	void toTransferReversedEvent_whenReversalIsNull_shouldMapFromOriginalTransferOnly() {
		TransferTransaction originalTransfer = TransferTransaction.builder().id(UUID.randomUUID())
				.sourceWalletId(UUID.randomUUID()).destinationWalletId(UUID.randomUUID())
				.amount(new BigDecimal("44.0000")).currency("USD").status(TransferStatus.REVERSED).reason("reason")
				.createdAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();

		TransferReversedEventDTO response = outboxEventMapper.toTransferReversedEvent(null, originalTransfer);

		assertNotNull(response);
		assertNull(response.reversalId());
		assertEquals(originalTransfer.getId(), response.originalTransferId());
		assertEquals(originalTransfer.getSourceWalletId(), response.sourceWalletId());
		assertEquals(originalTransfer.getDestinationWalletId(), response.destinationWalletId());
		assertEquals(originalTransfer.getAmount(), response.amount());
		assertEquals(originalTransfer.getCurrency(), response.currency());
		assertNull(response.reason());
		assertNull(response.occurredAt());
	}

	@Test
	void toTransferReversedEvent_whenOriginalTransferIsNull_shouldMapFromReversalOnly() {
		ReversalTransaction reversal = ReversalTransaction.builder().id(UUID.randomUUID())
				.originalTransferId(UUID.randomUUID()).status(ReversalStatus.COMPLETED).reason("fraud")
				.createdAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();

		TransferReversedEventDTO response = outboxEventMapper.toTransferReversedEvent(reversal, null);

		assertNotNull(response);
		assertEquals(reversal.getId(), response.reversalId());
		assertNull(response.originalTransferId());
		assertNull(response.sourceWalletId());
		assertNull(response.destinationWalletId());
		assertNull(response.amount());
		assertNull(response.currency());
		assertEquals(reversal.getReason(), response.reason());
		assertEquals(reversal.getCompletedAt(), response.occurredAt());
	}
}
