package com.mini.fintech.wallet_app.common.helper;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.JsonNodeFeature;

@TestConfiguration
public class JsonMapperTestConfig {

	@Bean
	public tools.jackson.databind.json.JsonMapper jsonMapper() {
		return tools.jackson.databind.json.JsonMapper.builder()
				.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES).build();
	}

}
