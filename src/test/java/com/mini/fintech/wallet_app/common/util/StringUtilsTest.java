package com.mini.fintech.wallet_app.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class StringUtilsTest {

	@Test
	void shouldNormalizeNullToEmptyString() {
		String actual = StringUtils.normalizeNullable((String) null);
		assertThat(actual).isEqualTo("");
	}

	@Test
	void shouldKeepProvidedReasonWhenNotBlank() {
		String actual = StringUtils.defaultString("customer_payment", "fallback_reason");

		assertThat(actual).isEqualTo("customer_payment");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void shouldReturnFallbackReasonWhenReasonIsNullOrBlank(String reason) {
		String actual = StringUtils.defaultString(reason, "fallback_reason");

		assertThat(actual).isEqualTo("fallback_reason");
	}

}
