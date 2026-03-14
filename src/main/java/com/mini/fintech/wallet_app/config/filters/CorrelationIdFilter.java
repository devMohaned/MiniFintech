package com.mini.fintech.wallet_app.config.filters;

import static com.mini.fintech.wallet_app.common.util.Constants.CORRELATION_ID_HEADER;
import static com.mini.fintech.wallet_app.common.util.Constants.CORRELATION_ID_KEY;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (!StringUtils.hasText(correlationId)) {
			correlationId = UUID.randomUUID().toString();
		}

		MDC.put(CORRELATION_ID_KEY, correlationId);
		response.setHeader(CORRELATION_ID_HEADER, correlationId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(CORRELATION_ID_KEY);
		}
	}
}
