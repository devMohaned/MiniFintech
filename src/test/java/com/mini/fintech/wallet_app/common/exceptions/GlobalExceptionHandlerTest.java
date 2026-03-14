package com.mini.fintech.wallet_app.common.exceptions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mini.fintech.wallet_app.common.dto.ApiErrorDTO;
import com.mini.fintech.wallet_app.common.exception.GlobalExceptionHandler;
import com.mini.fintech.wallet_app.common.factory.ApiErrorFactory;

class GlobalExceptionHandlerTest {

	@Test
	void shouldHandleUnexpectedExceptionAsInternalServerError() throws Exception {
		ApiErrorFactory apiErrorFactory = mock(ApiErrorFactory.class);
		when(apiErrorFactory.buildError("INTERNAL_ERROR", "Unexpected error occurred")).thenReturn(
				new ApiErrorDTO("INTERNAL_ERROR", "Unexpected error occurred", "corr-1", LocalDateTime.now()));

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BoomController())
				.setControllerAdvice(new GlobalExceptionHandler(apiErrorFactory)).build();

		mockMvc.perform(get("/boom").accept(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("Unexpected error occurred"));
	}

	@RestController
	static class BoomController {
		@GetMapping("/boom")
		String boom() {
			throw new RuntimeException("boom");
		}
	}
}
