package com.mini.fintech.wallet_app.common;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.mini.fintech.wallet_app.common.config.IntegrationTestContainersConfig;
import com.mini.fintech.wallet_app.common.helper.JsonMapperTestConfig;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
@Import({ IntegrationTestContainersConfig.class, JsonMapperTestConfig.class })
public abstract class BaseIntegrationTest {

	@Autowired
	protected TestRestTemplate restTemplate;

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("TRUNCATE TABLE wallet.dead_letter_events RESTART IDENTITY CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE wallet.outbox_events RESTART IDENTITY CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE wallet.reversal_transactions RESTART IDENTITY CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE wallet.idempotency_records RESTART IDENTITY CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE wallet.ledger_entries RESTART IDENTITY CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE wallet.transfer_transactions RESTART IDENTITY CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE wallet.wallet_accounts RESTART IDENTITY CASCADE");
	}

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.jpa.properties.hibernate.default_schema", () -> "wallet");
		registry.add("spring.flyway.default-schema", () -> "wallet");
		registry.add("spring.flyway.schemas", () -> "wallet");
	}

	protected String toJson(Object body) throws Exception {
		return objectMapper.writeValueAsString(body);
	}

	protected JsonNode readJson(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	protected MediaType json() {
		return MediaType.APPLICATION_JSON;
	}

	protected String createWallet(String customerId, String currency) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/wallets").contentType(json())
						.header("X-Correlation-Id", "wallet-create-helper-" + customerId)
						.content(toJson(TestDataFactory.createWalletRequest(customerId, currency))))
				.andExpect(status().isOk()).andReturn();

		JsonNode json = readJson(result);
		String walletId = json.get("id").asText();
		assertThat(walletId).isNotBlank();
		return walletId;
	}

	protected void creditWallet(String walletId, String amount) throws Exception {
		mockMvc.perform(post("/api/v1/wallets/{walletId}/credit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-credit-helper-" + walletId)
				.content(toJson(
						TestDataFactory.walletAmountRequest(amount, "EGP", "ref-credit-helper", "helper credit"))))
				.andExpect(status().isOk());
	}

	protected void debitWallet(String walletId, String amount) throws Exception {
		mockMvc.perform(post("/api/v1/wallets/{walletId}/debit", walletId).contentType(json())
				.header("X-Correlation-Id", "wallet-debit-helper-" + walletId).content(
						toJson(TestDataFactory.walletAmountRequest(amount, "EGP", "ref-debit-helper", "helper debit"))))
				.andExpect(status().isOk());
	}

	protected String createTransfer(String sourceWalletId, String destinationWalletId, String amount,
			String idempotencyKey, String reason) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/transfers").contentType(json())
						.header("X-Correlation-Id", "transfer-helper-" + idempotencyKey)
						.header("Idempotency-Key", idempotencyKey)
						.content(toJson(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
								UUID.fromString(destinationWalletId), amount, "EGP", reason))))
				.andExpect(status().isOk()).andReturn();

		JsonNode json = readJson(result);

		if (json.has("transferId")) {
			return json.get("transferId").asText();
		}
		if (json.has("id")) {
			return json.get("id").asText();
		}

		throw new IllegalStateException(
				"Could not resolve transfer id from response: " + result.getResponse().getContentAsString());
	}

	protected String reverseTransfer(String transferId, String reason) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/transfers/{transferId}/reversal", transferId).contentType(json())
						.header("X-Correlation-Id", "reversal-helper-" + transferId)
						.content(toJson(TestDataFactory.reversalRequest(reason))))
				.andExpect(status().isOk()).andReturn();

		JsonNode json = readJson(result);

		if (json.has("reversalId")) {
			return json.get("reversalId").asText();
		}
		if (json.has("id")) {
			return json.get("id").asText();
		}

		throw new IllegalStateException(
				"Could not resolve reversal id from response: " + result.getResponse().getContentAsString());
	}
}
