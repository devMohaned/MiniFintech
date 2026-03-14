package com.mini.fintech.wallet_app.wallet.service.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;
import com.mini.fintech.wallet_app.wallet.service.dto.WalletResponseDTO;

class WalletMapperTest {

	private final WalletMapper walletMapper = Mappers.getMapper(WalletMapper.class);

	@Test
	void toResponse_whenSourceIsNull_shouldReturnNull() {
		WalletResponseDTO response = walletMapper.toResponse(null);

		assertNull(response);
	}

	@Test
	void toResponse_whenSourceIsPresent_shouldMapFields() {
		UUID id = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		WalletAccount wallet = WalletAccount.builder().id(id).customerId("customer-1").currency("USD")
				.status(WalletStatus.ACTIVE).availableBalance(new BigDecimal("10.5000")).createdAt(now).updatedAt(now)
				.build();

		WalletResponseDTO response = walletMapper.toResponse(wallet);

		assertNotNull(response);
		assertEquals(id, response.id());
		assertEquals("customer-1", response.customerId());
		assertEquals("USD", response.currency());
	}
}
