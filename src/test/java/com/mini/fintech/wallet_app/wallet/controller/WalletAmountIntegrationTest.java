package com.mini.fintech.wallet_app.wallet.controller;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.annotation.DirtiesContext;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class WalletAmountIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldCreditWalletSuccessfully() throws Exception {
		String walletId = createWallet("cust_credit_001", "EGP");

		mockMvc.perform(post("/api/v1/wallets/{walletId}/credit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-credit-001")
				.content(toJson(
						TestDataFactory.walletAmountRequest("100.0000", "EGP", "ref-credit-001", "initial funding"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(walletId)).andExpect(
						jsonPath("$.availableBalance", comparesEqualTo(new BigDecimal("100.0000")), BigDecimal.class));
	}

	@Test
	void shouldDebitWalletSuccessfully() throws Exception {
		String walletId = createWallet("cust_debit_001", "EGP");
		creditWallet(walletId, "150.0000");

		mockMvc.perform(post("/api/v1/wallets/{walletId}/debit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-debit-001")
				.content(toJson(TestDataFactory.walletAmountRequest("50.0000", "EGP", "ref-debit-001", "purchase"))))
				.andExpect(status().isOk()).andExpect(
						jsonPath("$.availableBalance", comparesEqualTo(new BigDecimal("100.0000")), BigDecimal.class));
	}

	@Test
	void shouldRejectDebitWhenInsufficientFunds() throws Exception {
		String walletId = createWallet("cust_debit_002", "EGP");
		creditWallet(walletId, "20.0000");

		mockMvc.perform(post("/api/v1/wallets/{walletId}/debit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-debit-insufficient")
				.content(toJson(TestDataFactory.walletAmountRequest("50.0000", "EGP", "ref-debit-002", "purchase"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
	}

	@ParameterizedTest
	@CsvSource({ "0.0000", "-1.0000", "-100.0000" })
	void shouldRejectInvalidCreditAmounts(String amount) throws Exception {
		String walletId = createWallet("cust_invalid_credit", "EGP");

		mockMvc.perform(post("/api/v1/wallets/{walletId}/credit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-credit-invalid")
				.content(toJson(TestDataFactory.walletAmountRequest(amount, "EGP", "ref-credit-invalid", "bad input"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").exists());
	}

	@ParameterizedTest
	@CsvSource({ "USD", "KES", "LSL" })
	void shouldRejectCurrencyMismatchOnCredit(String requestCurrency) throws Exception {
		String walletId = createWallet("cust_currency_001", "EGP");

		mockMvc.perform(post("/api/v1/wallets/{walletId}/credit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-credit-currency")
				.content(toJson(TestDataFactory.walletAmountRequest("10.0000", requestCurrency, "ref-currency",
						"currency mismatch"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("currency")));
	}

}