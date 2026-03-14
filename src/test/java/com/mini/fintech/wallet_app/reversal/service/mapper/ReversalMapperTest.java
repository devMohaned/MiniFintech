package com.mini.fintech.wallet_app.reversal.service.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.mini.fintech.wallet_app.reversal.domain.ReversalStatus;
import com.mini.fintech.wallet_app.reversal.domain.ReversalTransaction;
import com.mini.fintech.wallet_app.reversal.service.dto.ReversalResponseDTO;

class ReversalMapperTest {

	private final ReversalMapper reversalMapper = Mappers.getMapper(ReversalMapper.class);

	@Test
	void toResponse_whenSourceIsNull_shouldReturnNull() {
		ReversalResponseDTO response = reversalMapper.toResponse(null);

		assertNull(response);
	}

	@Test
	void toResponse_whenSourceIsPresent_shouldMapIdToReversalId() {
		UUID reversalId = UUID.randomUUID();
		ReversalTransaction reversal = ReversalTransaction.builder().id(reversalId)
				.originalTransferId(UUID.randomUUID()).status(ReversalStatus.COMPLETED).reason("fraud")
				.createdAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();

		ReversalResponseDTO response = reversalMapper.toResponse(reversal);

		assertNotNull(response);
		assertEquals(reversalId, response.reversalId());
		assertEquals(reversal.getOriginalTransferId(), response.originalTransferId());
	}
}
