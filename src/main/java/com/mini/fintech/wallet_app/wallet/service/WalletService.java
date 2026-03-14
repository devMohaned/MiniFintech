package com.mini.fintech.wallet_app.wallet.service;

import static com.mini.fintech.wallet_app.common.util.ErrorCode.*;
import static com.mini.fintech.wallet_app.common.util.MoneyUtils.normalizeAmount;
import static com.mini.fintech.wallet_app.common.util.MoneyUtils.normalizeCurrency;
import static com.mini.fintech.wallet_app.common.util.StringUtils.defaultString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;
import com.mini.fintech.wallet_app.ledger.service.LedgerService;
import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;
import com.mini.fintech.wallet_app.wallet.repo.WalletAccountRepository;
import com.mini.fintech.wallet_app.wallet.service.dto.CreateWalletRequestDTO;
import com.mini.fintech.wallet_app.wallet.service.dto.WalletAmountRequestDTO;
import com.mini.fintech.wallet_app.wallet.service.dto.WalletResponseDTO;
import com.mini.fintech.wallet_app.wallet.service.mapper.WalletMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

	private final WalletAccountRepository walletAccountRepository;
	private final LedgerService ledgerService;
	private final WalletMapper walletMapper;

	@Transactional
	public WalletResponseDTO createWallet(CreateWalletRequestDTO request) {
		log.info("Started to create a wallet for Customer ID [{}] with Currency [{}].", request.getCustomerId(),
				request.getCurrency());
		try {
			String normalizedCurrency = normalizeCurrency(request.getCurrency());

			if (walletAccountRepository.existsByCustomerIdAndCurrency(request.getCustomerId(), normalizedCurrency)) {
				log.warn(
						"Rejected wallet creation because a wallet already exists for Customer ID [{}] and Currency [{}].",
						request.getCustomerId(), normalizedCurrency);
				throw new BusinessException(WALLET_ALREADY_EXISTS.name(),
						"Wallet already exists for customer and currency");
			}

			LocalDateTime now = LocalDateTime.now();
			WalletAccount wallet = WalletAccount.builder().id(UUID.randomUUID()).customerId(request.getCustomerId())
					.currency(normalizedCurrency).status(WalletStatus.ACTIVE)
					.availableBalance(BigDecimal.ZERO.setScale(4)).createdAt(now).updatedAt(now).build();

			WalletAccount saved = walletAccountRepository.save(wallet);
			log.info(
					"Finished creating wallet successfully. Wallet ID [{}], Customer ID [{}], Currency [{}], and Initial Balance [{}].",
					saved.getId(), saved.getCustomerId(), saved.getCurrency(), saved.getAvailableBalance());
			return walletMapper.toResponse(saved);
		} catch (Exception ex) {
			log.error("Failed to create wallet for Customer ID [{}] and Currency [{}].", request.getCustomerId(),
					request.getCurrency(), ex);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public WalletResponseDTO getWallet(UUID walletId) {
		log.info("Started to fetch wallet details for Wallet ID [{}].", walletId);
		try {
			WalletAccount wallet = walletAccountRepository.findById(walletId).orElseThrow(() -> {
				log.warn("Could not find wallet while fetching wallet details. Wallet ID [{}].", walletId);
				return new ResourceNotFoundException("Wallet not found: " + walletId);
			});
			log.info("Finished fetching wallet details. Wallet ID [{}], Status [{}], and Available Balance [{}].",
					wallet.getId(), wallet.getStatus(), wallet.getAvailableBalance());
			return walletMapper.toResponse(wallet);
		} catch (Exception ex) {
			log.error("Failed to fetch wallet details for Wallet ID [{}].", walletId, ex);
			throw ex;
		}
	}

	@Transactional
	public WalletResponseDTO creditWallet(UUID walletId, WalletAmountRequestDTO request) {
		log.info("Started to credit a wallet. Wallet ID [{}], Amount [{}], Currency [{}], and Reference [{}].",
				walletId, request.getAmount(), request.getCurrency(), request.getReference());
		try {
			WalletAccount wallet = loadActiveWallet(walletId);
			BigDecimal amount = normalizeAmount(request.getAmount());
			String currency = normalizeCurrency(request.getCurrency());

			validateCurrency(wallet, currency);

			wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
			wallet.setUpdatedAt(LocalDateTime.now());

			UUID transactionId = UUID.randomUUID();
			walletAccountRepository.save(wallet);
			log.info(
					"Wallet balance was updated after credit. Wallet ID [{}], New Balance [{}], and Transaction ID [{}].",
					wallet.getId(), wallet.getAvailableBalance(), transactionId);

			ledgerService.postEntry(transactionId, wallet.getId(), LedgerEntryType.CREDIT, amount, wallet.getCurrency(),
					LedgerReferenceType.TOP_UP, request.getReference(),
					defaultString(request.getDescription(), "Wallet credited"));

			log.info("Finished crediting wallet successfully. Wallet ID [{}] and Transaction ID [{}].", wallet.getId(),
					transactionId);
			return walletMapper.toResponse(wallet);
		} catch (Exception ex) {
			log.error("Failed to credit wallet. Wallet ID [{}], Amount [{}], Currency [{}], and Reference [{}].",
					walletId, request.getAmount(), request.getCurrency(), request.getReference(), ex);
			throw ex;
		}
	}

	@Transactional
	public WalletResponseDTO debitWallet(UUID walletId, WalletAmountRequestDTO request) {
		log.info("Started to debit a wallet. Wallet ID [{}], Amount [{}], Currency [{}], and Reference [{}].", walletId,
				request.getAmount(), request.getCurrency(), request.getReference());
		try {
			WalletAccount wallet = loadActiveWallet(walletId);
			BigDecimal amount = normalizeAmount(request.getAmount());
			String currency = normalizeCurrency(request.getCurrency());

			validateCurrency(wallet, currency);
			ensureSufficientFunds(wallet, amount);

			wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
			wallet.setUpdatedAt(LocalDateTime.now());

			UUID transactionId = UUID.randomUUID();
			walletAccountRepository.save(wallet);
			log.info(
					"Wallet balance was updated after debit. Wallet ID [{}], New Balance [{}], and Transaction ID [{}].",
					wallet.getId(), wallet.getAvailableBalance(), transactionId);

			ledgerService.postEntry(transactionId, wallet.getId(), LedgerEntryType.DEBIT, amount, wallet.getCurrency(),
					LedgerReferenceType.WITHDRAWAL, request.getReference(),
					defaultString(request.getDescription(), "Wallet debited"));

			log.info("Finished debiting wallet successfully. Wallet ID [{}] and Transaction ID [{}].", wallet.getId(),
					transactionId);
			return walletMapper.toResponse(wallet);
		} catch (Exception ex) {
			log.error("Failed to debit wallet. Wallet ID [{}], Amount [{}], Currency [{}], and Reference [{}].",
					walletId, request.getAmount(), request.getCurrency(), request.getReference(), ex);
			throw ex;
		}
	}

	private WalletAccount loadActiveWallet(UUID walletId) {
		log.debug("Checking wallet activity status before processing. Wallet ID [{}].", walletId);
		WalletAccount wallet = walletAccountRepository.findById(walletId).orElseThrow(() -> {
			log.warn("Could not find wallet while loading active wallet. Wallet ID [{}].", walletId);
			return new ResourceNotFoundException("Wallet not found: " + walletId);
		});

		if (wallet.getStatus() != WalletStatus.ACTIVE) {
			log.warn("Rejected operation because wallet is not active. Wallet ID [{}] and Status [{}].", walletId,
					wallet.getStatus());
			throw new BusinessException(WALLET_INACTIVE.name(), "Wallet is not active");
		}

		return wallet;
	}

	private void validateCurrency(WalletAccount wallet, String currency) {
		if (!wallet.getCurrency().equals(currency)) {
			log.warn(
					"Rejected operation due to currency mismatch. Wallet ID [{}], Wallet Currency [{}], and Request Currency [{}].",
					wallet.getId(), wallet.getCurrency(), currency);
			throw new BusinessException(INVALID_CURRENCY.name(), "Request currency does not match wallet currency");
		}
	}

	private void ensureSufficientFunds(WalletAccount wallet, BigDecimal amount) {
		if (wallet.getAvailableBalance().compareTo(amount) < 0) {
			log.warn(
					"Rejected debit operation due to insufficient funds. Wallet ID [{}], Available Balance [{}], and Requested Amount [{}].",
					wallet.getId(), wallet.getAvailableBalance(), amount);
			throw new BusinessException(INSUFFICIENT_FUNDS.name(), "Source wallet has insufficient balance");
		}
	}

}
