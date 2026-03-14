package com.mini.fintech.wallet_app.transfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

import tools.jackson.databind.JsonNode;

class TransferIdempotencyIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldReplaySameTransferForSameIdempotencyKey() throws Exception {
		String sourceWalletId = createWallet("cust_idem_src_001", "EGP");
		String destinationWalletId = createWallet("cust_idem_dst_001", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String idemKey = "idem-replay-001";

		MvcResult first = mockMvc
				.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "idem-first")
						.header("Idempotency-Key", idemKey)
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "100.0000", "EGP", "repeat_safe"))))
				.andExpect(status().isOk()).andReturn();

		MvcResult second = mockMvc
				.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "idem-second")
						.header("Idempotency-Key", idemKey)
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), "100.0000", "EGP", "repeat_safe"))))
				.andExpect(status().isOk()).andReturn();

		JsonNode firstJson = readJson(first);
		JsonNode secondJson = readJson(second);

		String firstTransferId = firstJson.has("transferId") ? firstJson.get("transferId").asText()
				: firstJson.get("id").asText();
		String secondTransferId = secondJson.has("transferId") ? secondJson.get("transferId").asText()
				: secondJson.get("id").asText();

		assertThat(secondTransferId).isEqualTo(firstTransferId);

		String sourceBalance = jdbcTemplate.queryForObject(
				"select available_balance::text from wallet.wallet_accounts where id = ?::uuid", String.class,
				sourceWalletId);
		String destinationBalance = jdbcTemplate.queryForObject(
				"select available_balance::text from wallet.wallet_accounts where id = ?::uuid", String.class,
				destinationWalletId);

		assertThat(sourceBalance).isEqualTo("400.0000");
		assertThat(destinationBalance).isEqualTo("100.0000");
	}

	@Test
	void shouldRejectSameIdempotencyKeyWithDifferentRequestBody() throws Exception {
		String sourceWalletId = createWallet("cust_idem_src_002", "EGP");
		String destinationWalletId = createWallet("cust_idem_dst_002", "EGP");
		creditWallet(sourceWalletId, "500.0000");

		String idemKey = "idem-conflict-001";

		mockMvc.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "idem-conflict-first")
				.header("Idempotency-Key", idemKey)
				.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
						UUID.fromString(destinationWalletId), "100.0000", "EGP", "first_body"))))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/transfers").contentType(json()).header("X-Correlation-Id", "idem-conflict-second")
				.header("Idempotency-Key", idemKey)
				.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
						UUID.fromString(destinationWalletId), "120.0000", "EGP", "different_body"))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
	}
}
