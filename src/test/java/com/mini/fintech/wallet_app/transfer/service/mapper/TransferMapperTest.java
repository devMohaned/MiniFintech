package com.mini.fintech.wallet_app.transfer.service.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;
import com.mini.fintech.wallet_app.transfer.service.dto.TransferResponseDTO;

class TransferMapperTest {

	private final TransferMapper transferMapper = Mappers.getMapper(TransferMapper.class);

	@Test
	void toResponse_whenSourceIsNull_shouldReturnNull() {
		TransferResponseDTO response = transferMapper.toResponse(null);

		assertNull(response);
	}

	@Test
	void toResponse_whenSourceIsPresent_shouldMapIdToTransferId() {
		UUID transferId = UUID.randomUUID();
		TransferTransaction transfer = TransferTransaction.builder().id(transferId).sourceWalletId(UUID.randomUUID())
				.destinationWalletId(UUID.randomUUID()).amount(new BigDecimal("15.0000")).currency("USD")
				.status(TransferStatus.COMPLETED).reason("test").createdAt(LocalDateTime.now())
				.completedAt(LocalDateTime.now()).build();

		TransferResponseDTO response = transferMapper.toResponse(transfer);

		assertNotNull(response);
		assertEquals(transferId, response.transferId());
		assertEquals(transfer.getSourceWalletId(), response.sourceWalletId());
	}
}
