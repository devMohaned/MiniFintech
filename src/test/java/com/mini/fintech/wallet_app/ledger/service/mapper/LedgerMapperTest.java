package com.mini.fintech.wallet_app.ledger.service.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.mini.fintech.wallet_app.ledger.domain.LedgerEntry;
import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;
import com.mini.fintech.wallet_app.ledger.service.dto.LedgerEntryResponseDTO;

class LedgerMapperTest {

	private final LedgerMapper ledgerMapper = Mappers.getMapper(LedgerMapper.class);

	@Test
	void toResponse_whenSourceIsNull_shouldReturnNull() {
		LedgerEntryResponseDTO response = ledgerMapper.toResponse(null);

		assertNull(response);
	}

	@Test
	void toResponse_whenSourceIsPresent_shouldMapFields() {
		LedgerEntry entry = LedgerEntry.builder().id(UUID.randomUUID()).transactionId(UUID.randomUUID())
				.walletId(UUID.randomUUID()).entryType(LedgerEntryType.CREDIT).amount(new BigDecimal("20.0000"))
				.currency("USD").referenceType(LedgerReferenceType.TRANSFER).referenceId("ref-1").description("desc")
				.createdAt(LocalDateTime.now()).build();

		LedgerEntryResponseDTO response = ledgerMapper.toResponse(entry);

		assertNotNull(response);
		assertEquals(entry.getId(), response.id());
		assertEquals(entry.getReferenceId(), response.referenceId());
	}
}
