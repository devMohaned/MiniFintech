package com.mini.fintech.wallet_app.reversal.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mini.fintech.wallet_app.reversal.service.ReversalService;
import com.mini.fintech.wallet_app.reversal.service.dto.CreateReversalRequestDTO;
import com.mini.fintech.wallet_app.reversal.service.dto.ReversalResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class ReversalController {

	private final ReversalService reversalService;

	@PostMapping("/{transferId}/reversal")
	public ResponseEntity<ReversalResponseDTO> reverseTransfer(@PathVariable UUID transferId,
			@Valid @RequestBody CreateReversalRequestDTO request) {
		ReversalResponseDTO response = reversalService.reverseTransfer(transferId, request);
		return ResponseEntity.ok(response);
	}
}
