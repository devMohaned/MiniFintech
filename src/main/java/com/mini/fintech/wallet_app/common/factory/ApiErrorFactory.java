package com.mini.fintech.wallet_app.common.factory;

import static com.mini.fintech.wallet_app.common.util.Constants.CORRELATION_ID_KEY;

import java.time.LocalDateTime;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.mini.fintech.wallet_app.common.dto.ApiErrorDTO;

@Component
public class ApiErrorFactory {

	public ApiErrorDTO buildError(String code, String message) {
		return new ApiErrorDTO(code, message, MDC.get(CORRELATION_ID_KEY), LocalDateTime.now());
	}
}
