package com.mini.fintech.wallet_app.common.exception;

import static com.mini.fintech.wallet_app.common.util.ErrorCode.*;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mini.fintech.wallet_app.common.dto.ApiErrorDTO;
import com.mini.fintech.wallet_app.common.factory.ApiErrorFactory;
import com.mini.fintech.wallet_app.common.util.ErrorUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

	private final ApiErrorFactory apiErrorFactory;

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException ex) {
		log.error("Handled resource not found exception with message [{}].", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(apiErrorFactory.buildError(RESOURCE_NOT_FOUND.name(), ex.getMessage()));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorDTO> handleBusiness(BusinessException ex) {
		HttpStatus status = CONFLICT_CODES.contains(ex.getCode()) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

		log.error("Handled business exception with code [{}], message [{}], and HTTP status [{}].", ex.getCode(),
				ex.getMessage(), status.value());

		return ResponseEntity.status(status).body(apiErrorFactory.buildError(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ApiErrorDTO> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
		log.error(
				"Handled optimistic-locking exception and returned conflict response. Rolling back database transaction",
				ex);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(apiErrorFactory
				.buildError(CONCURRENT_MODIFICATION.name(), "Resource was modified concurrently. Please retry"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getAllErrors().stream().map(ErrorUtils::formatError)
				.collect(Collectors.joining(", "));

		log.error("Handled validation exception and returned message [{}].", message, ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(apiErrorFactory.buildError(VALIDATION_ERROR.name(), message));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorDTO> handleValidation(IllegalArgumentException ex) {
		log.error("Handled illegal argument exception and returned message [{}].", ex.getMessage(), ex);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(apiErrorFactory.buildError(VALIDATION_ERROR.name(), ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Handled unexpected exception for request URI [{}] and method [{}].", request.getRequestURI(),
				request.getMethod(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(apiErrorFactory.buildError(INTERNAL_ERROR.name(), "Unexpected error occurred"));
	}

}
