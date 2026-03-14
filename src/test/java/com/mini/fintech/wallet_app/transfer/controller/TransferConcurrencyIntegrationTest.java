package com.mini.fintech.wallet_app.transfer.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.helper.TestDataFactory;

import tools.jackson.databind.JsonNode;

class TransferConcurrencyIntegrationTest extends BaseIntegrationTest {

	@Test
	void shouldHandleConcurrentTransfersSafely() throws Exception {
		String sourceWalletId = createWallet("cust_conc_src", "EGP");
		String destinationWalletId = createWallet("cust_conc_dst", "EGP");
		creditWallet(sourceWalletId, "150.0000");

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		List<ResponseEntity<String>> responses = new CopyOnWriteArrayList<>();

		Callable<Void> task1 = () -> {
			ready.countDown();
			start.await();
			responses.add(sendTransfer(sourceWalletId, destinationWalletId, "100.0000", "conc-idem-001", "conc-1"));
			return null;
		};

		Callable<Void> task2 = () -> {
			ready.countDown();
			start.await();
			responses.add(sendTransfer(sourceWalletId, destinationWalletId, "50.0000", "conc-idem-002", "conc-2"));
			return null;
		};

		Future<Void> f1 = executor.submit(task1);
		Future<Void> f2 = executor.submit(task2);

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();

		f1.get(10, TimeUnit.SECONDS);
		f2.get(10, TimeUnit.SECONDS);
		executor.shutdown();

		long successCount = responses.stream().filter(r -> r.getStatusCode() == HttpStatus.OK).count();
		long failureCount = responses.stream().filter(r -> r.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)).count();

		JsonNode sourceWallet = getWallet(sourceWalletId);
		JsonNode destinationWallet = getWallet(destinationWalletId);

		assertThat(successCount).isGreaterThanOrEqualTo(1);
		assertThat(successCount + failureCount).isEqualTo(2);
		assertThat(sourceWallet.get("availableBalance").decimalValue()).isIn(new BigDecimal("50.0000"),
				new BigDecimal("100.0000"));
		assertThat(destinationWallet.get("availableBalance").decimalValue()).isIn(new BigDecimal("100.0000"),
				new BigDecimal("50.0000"));
	}

	private ResponseEntity<String> sendTransfer(String sourceWalletId, String destinationWalletId, String amount,
			String idempotencyKey, String correlationId) {
		HttpHeaders headers = jsonHeaders(correlationId);
		headers.set("Idempotency-Key", idempotencyKey);
		return restTemplate.postForEntity("/api/v1/transfers",
				new HttpEntity<>(TestDataFactory.transferRequest(UUID.fromString(sourceWalletId),
						UUID.fromString(destinationWalletId), amount, "EGP", "concurrency_test"), headers),
				String.class);
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
