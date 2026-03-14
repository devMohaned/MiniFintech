package com.mini.fintech.wallet_app.transfer.controller;

import static com.mini.fintech.wallet_app.common.util.ErrorCode.IDEMPOTENCY_KEY_REQUIRED;
import static com.mini.fintech.wallet_app.idemptotency.util.Constants.IDEMPOTENCY_KEY_HEADER;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.transfer.service.TransferService;
import com.mini.fintech.wallet_app.transfer.service.dto.CreateTransferRequestDTO;
import com.mini.fintech.wallet_app.transfer.service.dto.TransferResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

	private final TransferService transferService;

	@PostMapping
	public ResponseEntity<TransferResponseDTO> createTransfer(@Valid @RequestBody CreateTransferRequestDTO request,
			@RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
		if (!StringUtils.hasText(idempotencyKey)) {
			log.warn("Rejected transfer API request because Idempotency-Key header is missing.");
			throw new BusinessException(IDEMPOTENCY_KEY_REQUIRED.name(),
					"Idempotency-Key header is required for transfer creation");
		}

		TransferResponseDTO response = transferService.createTransfer(request, idempotencyKey.trim());
		return ResponseEntity.ok(response);

	}

}
