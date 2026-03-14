package com.mini.fintech.wallet_app.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class MoneyUtils {

	private MoneyUtils() {
	}

	public static BigDecimal normalizeAmount(BigDecimal amount) {
		return amount.setScale(4, RoundingMode.HALF_UP);
	}

	public static String normalizeCurrency(String currency) {
		return currency.toUpperCase(Locale.ROOT);
	}
}
