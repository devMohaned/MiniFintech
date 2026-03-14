package com.mini.fintech.wallet_app.wallet.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mini.fintech.wallet_app.ledger.service.LedgerService;
import com.mini.fintech.wallet_app.ledger.service.dto.LedgerEntryResponseDTO;
import com.mini.fintech.wallet_app.wallet.service.WalletService;
import com.mini.fintech.wallet_app.wallet.service.dto.CreateWalletRequestDTO;
import com.mini.fintech.wallet_app.wallet.service.dto.WalletAmountRequestDTO;
import com.mini.fintech.wallet_app.wallet.service.dto.WalletResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

	private final WalletService walletService;
	private final LedgerService ledgerService;

	@PostMapping
	public ResponseEntity<WalletResponseDTO> createWallet(@Valid @RequestBody CreateWalletRequestDTO request) {

		WalletResponseDTO response = walletService.createWallet(request);

		return ResponseEntity.ok(response);

	}

	@GetMapping("/{walletId}")
	public ResponseEntity<WalletResponseDTO> getWallet(@PathVariable UUID walletId) {

		WalletResponseDTO response = walletService.getWallet(walletId);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/{walletId}/credit")
	public ResponseEntity<WalletResponseDTO> creditWallet(@PathVariable UUID walletId,
			@Valid @RequestBody WalletAmountRequestDTO request) {
		WalletResponseDTO response = walletService.creditWallet(walletId, request);
		return ResponseEntity.ok(response);

	}

	@PostMapping("/{walletId}/debit")
	public ResponseEntity<WalletResponseDTO> debitWallet(@PathVariable UUID walletId,
			@Valid @RequestBody WalletAmountRequestDTO request) {

		WalletResponseDTO response = walletService.debitWallet(walletId, request);
		return ResponseEntity.ok(response);

	}

	@GetMapping("/{walletId}/ledger")
	public ResponseEntity<Page<LedgerEntryResponseDTO>> getWalletLedger(@PathVariable UUID walletId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<LedgerEntryResponseDTO> response = ledgerService.getWalletLedger(walletId, pageable);
		return ResponseEntity.ok(response);

	}
}
