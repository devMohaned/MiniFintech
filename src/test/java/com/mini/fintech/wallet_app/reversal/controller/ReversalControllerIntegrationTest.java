package com.mini.fintech.wallet_app.reversal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

import tools.jackson.databind.JsonNode;

class ReversalControllerIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldReverseCompletedTransfer() throws Exception {
		String sourceWalletId = createWallet("cust_rev_src", "EGP");
		String destinationWalletId = createWallet("cust_rev_dst", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String transferId = createTransfer(sourceWalletId, destinationWalletId, "150.0000", "idem-reversal-001",
				"a Transfer");

		HttpEntity<Map<String, Object>> reversalRequest = new HttpEntity<>(
				TestDataFactory.reversalRequest("customer_dispute"), jsonHeaders("reversal-001"));

		ResponseEntity<String> reversalResponse = restTemplate
				.postForEntity("/api/v1/transfers/" + transferId + "/reversal", reversalRequest, String.class);

		assertThat(reversalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(reversalResponse.getBody()).contains("COMPLETED");

		JsonNode sourceWallet = getWallet(sourceWalletId);
		JsonNode destinationWallet = getWallet(destinationWalletId);
		assertThat(sourceWallet.get("availableBalance").asText()).isEqualTo("500.0000");
		assertThat(destinationWallet.get("availableBalance").asText()).isEqualTo("0.0000");
	}

	@Test
	void shouldReverseCompletedTransferSuccessfully() throws Exception {
		String sourceWalletId = createWallet("cust_rev_src_001", "EGP");
		String destinationWalletId = createWallet("cust_rev_dst_001", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String transferId = createTransfer(sourceWalletId, destinationWalletId, "150.0000", "idem-reversal-001",
				"normal_transfer");

		mockMvc.perform(post("/api/v1/transfers/{transferId}/reversal", transferId).contentType(json())
				.header("X-Correlation-Id", "reversal-001")
				.content(toJson(java.util.Map.of("reason", "customer_dispute")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.originalTransferId").value(transferId))
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		String sourceBalance = jdbcTemplate.queryForObject(
				"select available_balance::text from wallet.wallet_accounts where id = ?::uuid", String.class,
				sourceWalletId);
		String destinationBalance = jdbcTemplate.queryForObject(
				"select available_balance::text from wallet.wallet_accounts where id = ?::uuid", String.class,
				destinationWalletId);

		assertThat(sourceBalance).isEqualTo("500.0000");
		assertThat(destinationBalance).isEqualTo("0.0000");
	}

	@Test
	void shouldRejectReversalWhenTransferDoesNotExist() throws Exception {
		mockMvc.perform(post("/api/v1/transfers/{transferId}/reversal", java.util.UUID.randomUUID()).contentType(json())
				.header("X-Correlation-Id", "reversal-404")
				.content(toJson(java.util.Map.of("reason", "customer_dispute")))).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").exists());
	}

	@Test
	void shouldRejectAlreadyReversedTransfer() throws Exception {
		String sourceWalletId = createWallet("cust_rev_src_002", "EGP");
		String destinationWalletId = createWallet("cust_rev_dst_002", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String transferId = createTransfer(sourceWalletId, destinationWalletId, "100.0000", "idem-reversal-002",
				"normal_transfer");

		reverseTransfer(transferId, "customer_dispute");

		mockMvc.perform(post("/api/v1/transfers/{transferId}/reversal", transferId).contentType(json())
				.header("X-Correlation-Id", "reversal-duplicate")
				.content(toJson(java.util.Map.of("reason", "second_attempt")))).andExpect(status().isConflict());
	}

	@Test
	void shouldRejectReversalWhenDestinationWalletHasNoFunds() throws Exception {
		String sourceWalletId = createWallet("cust_rev_src_003", "EGP");
		String destinationWalletId = createWallet("cust_rev_dst_003", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String transferId = createTransfer(sourceWalletId, destinationWalletId, "100.0000", "idem-reversal-003",
				"normal_transfer");

		debitWallet(destinationWalletId, "100.0000");

		mockMvc.perform(post("/api/v1/transfers/{transferId}/reversal", transferId).contentType(json())
				.header("X-Correlation-Id", "reversal-no-funds")
				.content(toJson(java.util.Map.of("reason", "customer_dispute")))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REVERSAL_INSUFFICIENT_FUNDS"));
	}

	@Test
	void shouldRejectReversalWhenDestinationWalletIsInactive() throws Exception {
		String sourceWalletId = createWallet("cust_rev_inactive_src", "EGP");
		String destinationWalletId = createWallet("cust_rev_inactive_dst", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String transferId = createTransfer(sourceWalletId, destinationWalletId, "100.0000", "idem-reversal-inactive",
				"normal_transfer");

		jdbcTemplate.update("update wallet.wallet_accounts set status = 'SUSPENDED' where id = ?::uuid",
				destinationWalletId);

		mockMvc.perform(post("/api/v1/transfers/{transferId}/reversal", transferId).contentType(json())
				.header("X-Correlation-Id", "reversal-inactive-wallet")
				.content(toJson(TestDataFactory.reversalRequest("customer_dispute"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WALLET_INACTIVE"));
	}

	private JsonNode getWallet(String walletId) throws Exception {
		return objectMapper.readTree(restTemplate.getForObject("/api/v1/wallets/" + walletId, String.class));
	}

	private HttpHeaders jsonHeaders(String correlationId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Correlation-Id", correlationId);
		return headers;
	}
}