package com.mini.fintech.wallet_app.transfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.*;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

import tools.jackson.databind.JsonNode;

class TransferControllerIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldTransferSuccessfully() throws Exception {
		String sourceWalletId = createWallet("cust_transfer_src_001", "EGP");
		String destinationWalletId = createWallet("cust_transfer_dst_001", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		mockMvc.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-001")
				.header("Idempotency-Key", "idem-transfer-001")
				.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
						UUID.fromString(destinationWalletId), "150.0000", "EGP", "peer_transfer"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.sourceWalletId").value(sourceWalletId))
				.andExpect(jsonPath("$.destinationWalletId").value(destinationWalletId))
				.andExpect(jsonPath("$.amount", comparesEqualTo(new BigDecimal("150.0000")), BigDecimal.class));
	}

	@Test
	void shouldRejectMissingIdempotencyKey() throws Exception {
		String sourceWalletId = createWallet("cust_transfer_src_002", "EGP");
		String destinationWalletId = createWallet("cust_transfer_dst_002", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		mockMvc.perform(
				post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-missing-idem")
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "100.0000", "EGP", "peer_transfer"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectSameSourceAndDestinationWallet() throws Exception {
		String walletId = createWallet("cust_transfer_same_001", "EGP");
		creditWallet(walletId, "500.0000");

		mockMvc.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-same-wallet")
				.header("Idempotency-Key", "idem-transfer-same-wallet")
				.content(toJson(TestDataFactory.transferRequest(UUID.fromString(walletId), UUID.fromString(walletId),
						"100.0000", "EGP", "invalid_same_wallet"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TRANSFER"));
	}

	@Test
	void shouldRejectWhenSourceWalletHasInsufficientFunds() throws Exception {
		String sourceWalletId = createWallet("cust_transfer_src_003", "EGP");
		String destinationWalletId = createWallet("cust_transfer_dst_003", "EGP");
		creditWallet(sourceWalletId, "50.0000");

		mockMvc.perform(
				post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-insufficient")
						.header("Idempotency-Key", "idem-transfer-insufficient")
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "100.0000", "EGP", "overdraft_attempt"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
	}

	@Test
	void shouldRejectWhenDestinationWalletDoesNotExist() throws Exception {
		String sourceWalletId = createWallet("cust_transfer_src_004", "EGP");
		creditWallet(sourceWalletId, "200.0000");

		mockMvc.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-dst-404")
				.header("Idempotency-Key", "idem-transfer-dst-404")
				.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId), UUID.randomUUID(),
						"50.0000", "EGP", "missing_destination"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").exists());
	}

	@ParameterizedTest
	@CsvSource({ "0.0000", "-1.0000", "-100.0000" })
	void shouldRejectInvalidTransferAmounts(String amount) throws Exception {
		String sourceWalletId = createWallet("cust_transfer_src_invalid_" + amount.replace("-", "n").replace(".", "_"),
				"EGP");
		String destinationWalletId = createWallet(
				"cust_transfer_dst_invalid_" + amount.replace("-", "n").replace(".", "_"), "EGP");
		creditWallet(sourceWalletId, "200.0000");

		mockMvc.perform(
				post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-invalid-amount")
						.header("Idempotency-Key", "idem-transfer-invalid-" + amount)
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), amount, "EGP", "invalid_amount"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldRejectCurrencyMismatchBetweenTransferAndWallet() throws Exception {
		String sourceWalletId = createWallet("cust_transfer_src_005", "EGP");
		String destinationWalletId = createWallet("cust_transfer_dst_005", "EGP");
		creditWallet(sourceWalletId, "200.0000");

		mockMvc.perform(
				post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-currency-mismatch")
						.header("Idempotency-Key", "idem-transfer-currency")
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "50.0000", "USD", "wrong_currency"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("currency")));
	}

	@Test
	void shouldReplaySameTransferForSameIdempotencyKey() throws Exception {
		String sourceWalletId = createWallet("cust_src_idem", "EGP");
		String destinationWalletId = createWallet("cust_dst_idem", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		HttpHeaders headers = jsonHeaders("idem-replay-001");
		headers.set("Idempotency-Key", "idem-replay-key-001");

		Map<String, Object> body = TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
				UUID.fromString(destinationWalletId), "100.0000", "EGP", "repeat_safe");

		ResponseEntity<String> first = restTemplate.postForEntity("/api/v1/transfers", new HttpEntity<>(body, headers),
				String.class);
		ResponseEntity<String> second = restTemplate.postForEntity("/api/v1/transfers", new HttpEntity<>(body, headers),
				String.class);

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

		JsonNode firstJson = objectMapper.readTree(first.getBody());
		JsonNode secondJson = objectMapper.readTree(second.getBody());
		assertThat(secondJson.get("transferId").asText()).isEqualTo(firstJson.get("transferId").asText());

		JsonNode sourceWallet = getWallet(sourceWalletId);
		JsonNode destinationWallet = getWallet(destinationWalletId);
		assertThat(sourceWallet.get("availableBalance").asText()).isEqualTo("400.0000");
		assertThat(destinationWallet.get("availableBalance").asText()).isEqualTo("100.0000");
	}

	@Test
	void shouldRejectSameIdempotencyKeyWithDifferentBody() throws Exception {
		String sourceWalletId = createWallet("cust_src_idem2", "EGP");
		String destinationWalletId = createWallet("cust_dst_idem2", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		HttpHeaders headers = jsonHeaders("idem-conflict-001");
		headers.set("Idempotency-Key", "idem-conflict-key-001");

		ResponseEntity<String> first = restTemplate
				.postForEntity("/api/v1/transfers",
						new HttpEntity<>(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "100.0000", "EGP", "first"), headers),
						String.class);

		ResponseEntity<String> second = restTemplate
				.postForEntity("/api/v1/transfers",
						new HttpEntity<>(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "120.0000", "EGP", "second"), headers),
						String.class);

		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(second.getBody()).contains("IDEMPOTENCY_KEY_REUSED");
	}

	@Test
	void shouldRejectTransferWhenSourceWalletIsInactive() throws Exception {
		String sourceWalletId = createWallet("cust_transfer_inactive_src", "EGP");
		String destinationWalletId = createWallet("cust_transfer_inactive_dst", "EGP");
		creditWallet(sourceWalletId, "200.0000");

		jdbcTemplate.update("update wallet.wallet_accounts set status = 'SUSPENDED' where id = ?::uuid",
				sourceWalletId);

		mockMvc.perform(
				post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-inactive-src")
						.header("Idempotency-Key", "idem-transfer-inactive-src")
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "50.0000", "EGP", "inactive_wallet"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WALLET_INACTIVE"));
	}

	@Test
	void shouldReturnNotFoundWhenTransferDoesNotExist() throws Exception {
		mockMvc.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "transfer-get-404")
				.header("Idempotency-Key", "idem-transfer-inactive-src").content(toJson(TestDataFactory
						.transferRequest(UUID.randomUUID(), UUID.randomUUID(), "50.0000", "EGP", "inactive_wallet"))))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").exists());
	}

	private JsonNode getWallet(String walletId) {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/wallets/" + walletId, String.class);
		return objectMapper.readTree(response.getBody());
	}

	private HttpHeaders jsonHeaders(String correlationId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Correlation-Id", correlationId);
		return headers;
	}
}