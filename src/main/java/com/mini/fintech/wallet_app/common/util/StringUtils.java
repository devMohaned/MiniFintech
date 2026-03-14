package com.mini.fintech.wallet_app.common.util;

public class StringUtils {
	private StringUtils() {
	}

	public static String normalizeNullable(String value) {
		return value == null ? "" : value.trim();
	}

	public static String defaultString(String str, String fallback) {
		return (str == null || str.isBlank()) ? fallback : str;
	}

}
