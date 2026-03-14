package com.mini.fintech.wallet_app.ledger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;
import com.mini.fintech.wallet_app.ledger.repo.LedgerEntryRepository;
import com.mini.fintech.wallet_app.ledger.service.LedgerService;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

	@Mock
	private LedgerEntryRepository ledgerEntryRepository;

	@InjectMocks
	private LedgerService ledgerService;

	@Test
	void shouldRethrowWhenPostingLedgerEntryFails() {
		doThrow(new RuntimeException("ledger save failed")).when(ledgerEntryRepository).save(any());

		assertThatThrownBy(() -> ledgerService.postEntry(UUID.randomUUID(), UUID.randomUUID(), LedgerEntryType.DEBIT,
				new BigDecimal("10.0000"), "EGP", LedgerReferenceType.TRANSFER, "test_ref", "test_desc"))
				.isInstanceOf(RuntimeException.class).hasMessageContaining("ledger save failed");
	}
}
