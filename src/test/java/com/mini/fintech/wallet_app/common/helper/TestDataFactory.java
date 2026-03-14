package com.mini.fintech.wallet_app.common.helper;

import java.util.Map;
import java.util.UUID;

public final class TestDataFactory {

	private TestDataFactory() {
	}

	public static Map<String, Object> createWalletRequest(String customerId, String currency) {
		return Map.of("customerId", customerId, "currency", currency);
	}

	public static Map<String, Object> walletAmountRequest(String amount, String currency, String reference,
			String description) {
		return Map.of("amount", amount, "currency", currency, "reference", reference, "description", description);
	}

	public static Map<String, Object> transferRequest(UUID sourceWalletId, UUID destinationWalletId, String amount,
			String currency, String reason) {
		return Map.of("sourceWalletId", sourceWalletId, "destinationWalletId", destinationWalletId, "amount", amount,
				"currency", currency, "reason", reason);
	}

	public static Map<String, Object> reversalRequest(String reason) {
		return Map.of("reason", reason);
	}
}