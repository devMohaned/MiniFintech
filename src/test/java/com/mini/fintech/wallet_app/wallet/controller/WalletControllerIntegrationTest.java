package com.mini.fintech.wallet_app.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

class WalletControllerIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldCreateWallet() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Correlation-Id", "test-wallet-create-001");

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(
				TestDataFactory.createWalletRequest("cust_test_1", "EGP"), headers);

		ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/wallets", request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("cust_test_1");
		assertThat(response.getBody()).contains("EGP");
		assertThat(response.getBody()).contains("ACTIVE");
	}

	@Test
	void shouldCreateWalletSuccessfully() throws Exception {
		mockMvc.perform(
				post(URI.create("/api/v1/wallets")).contentType(json()).header("X-Correlation-Id", "wallet-create-001")
						.content(toJson(TestDataFactory.createWalletRequest("cust_001", "EGP"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.customerId").value("cust_001"))
				.andExpect(jsonPath("$.currency").value("EGP")).andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.id").isNotEmpty()).andExpect(jsonPath("$.availableBalance").exists());
	}

	@Test
	void shouldGetWalletByIdSuccessfully() throws Exception {
		String walletId = createWallet("cust_get_001", "EGP");

		mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId).header("X-Correlation-Id", "wallet-get-001"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(walletId))
				.andExpect(jsonPath("$.customerId").value("cust_get_001"))
				.andExpect(jsonPath("$.currency").value("EGP"));
	}

	@Test
	void shouldReturnNotFoundWhenWalletDoesNotExist() throws Exception {
		mockMvc.perform(
				get("/api/v1/wallets/{walletId}", UUID.randomUUID()).header("X-Correlation-Id", "wallet-get-404"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").exists());
	}

	@ParameterizedTest
	@MethodSource("invalidCreateWalletRequests")
	void shouldRejectInvalidCreateWalletRequest(Map<String, Object> body, String expectedFragment) throws Exception {
		mockMvc.perform(post(URI.create("/api/v1/wallets")).contentType(json())
				.header("X-Correlation-Id", "wallet-create-invalid").content(toJson(body)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(expectedFragment)));
	}

	static Stream<org.junit.jupiter.params.provider.Arguments> invalidCreateWalletRequests() {
		return Stream.of(org.junit.jupiter.params.provider.Arguments.of(Map.of("currency", "EGP"), "customerId"),
				org.junit.jupiter.params.provider.Arguments.of(Map.of("customerId", "cust_x"), "currency"),
				org.junit.jupiter.params.provider.Arguments.of(Map.of("customerId", "", "currency", "EGP"),
						"customerId"),
				org.junit.jupiter.params.provider.Arguments.of(Map.of("customerId", "cust_x", "currency", ""),
						"currency"));
	}

	@Test
	void shouldRejectDuplicateWalletForSameCustomerAndCurrency() throws Exception {
		mockMvc.perform(post("/api/v1/wallets").contentType(json()).header("X-Correlation-Id", "wallet-dup-001")
				.content(toJson(TestDataFactory.createWalletRequest("cust_dup_001", "EGP"))))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/wallets").contentType(json()).header("X-Correlation-Id", "wallet-dup-002")
				.content(toJson(TestDataFactory.createWalletRequest("cust_dup_001", "EGP"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WALLET_ALREADY_EXISTS"));
	}
}