package com.mini.fintech.wallet_app.wallet.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;

class WalletLedgerIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldReturnLedgerEntriesForWallet() throws Exception {
		String walletId = createWallet("cust_ledger_001", "EGP");

		creditWallet(walletId, "100.0000");
		debitWallet(walletId, "20.0000");

		mockMvc.perform(get("/api/v1/wallets/{walletId}/ledger", walletId)
				.header("X-Correlation-Id", "wallet-ledger-001").param("page", "0").param("size", "20"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.totalElements", is(2)))
				.andExpect(jsonPath("$.content[0].walletId").value(walletId))
				.andExpect(jsonPath("$.content[1].walletId").value(walletId));
	}

	@Test
	void shouldReturnEmptyLedgerWhenNoEntriesExist() throws Exception {
		String walletId = createWallet("cust_ledger_002", "EGP");

		mockMvc.perform(get("/api/v1/wallets/{walletId}/ledger", walletId)
				.header("X-Correlation-Id", "wallet-ledger-empty").param("page", "0").param("size", "20"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)))
				.andExpect(jsonPath("$.totalElements", is(0)));
	}

	@Test
	void shouldReturnNotFoundWhenLedgerWalletDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/v1/wallets/{walletId}/ledger", java.util.UUID.randomUUID())
				.header("X-Correlation-Id", "wallet-ledger-404").param("page", "0").param("size", "20"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").exists());
	}

	@ParameterizedTest
	@CsvSource({ "-1, 20", "0, 0", "0, -1" })
	void shouldRejectInvalidLedgerPagingParameters(int page, int size) throws Exception {
		String walletId = createWallet("cust_ledger_invalid_" + Math.abs(page) + "_" + Math.abs(size), "EGP");

		mockMvc.perform(get("/api/v1/wallets/{walletId}/ledger", walletId)
				.header("X-Correlation-Id", "wallet-ledger-invalid-page").param("page", String.valueOf(page))
				.param("size", String.valueOf(size))).andExpect(status().isBadRequest());
	}
}