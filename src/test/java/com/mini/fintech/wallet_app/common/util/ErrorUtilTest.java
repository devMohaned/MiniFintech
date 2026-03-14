package com.mini.fintech.wallet_app.common.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.ObjectError;

class ErrorUtilTest {

	@Test
	void shouldFormatObjectErrorWhenNotFieldError() {
		ObjectError error = new ObjectError("transferRequest", "generic validation failure");
		String actual = ErrorUtils.formatError(error);

		assertThat(actual).isEqualTo("generic validation failure");
	}
}
