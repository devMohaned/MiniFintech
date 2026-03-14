package com.mini.fintech.wallet_app.transfer.service;

import static com.mini.fintech.wallet_app.common.util.ErrorCode.*;
import static com.mini.fintech.wallet_app.common.util.MoneyUtils.normalizeAmount;
import static com.mini.fintech.wallet_app.common.util.MoneyUtils.normalizeCurrency;
import static com.mini.fintech.wallet_app.common.util.StringUtils.defaultString;
import static com.mini.fintech.wallet_app.transfer.domain.TransferOperations.TRANSFER_CREATE;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyRecord;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyStatus;
import com.mini.fintech.wallet_app.idemptotency.service.IdempotencyService;
import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;
import com.mini.fintech.wallet_app.ledger.service.LedgerService;
import com.mini.fintech.wallet_app.outbox.service.OutboxService;
import com.mini.fintech.wallet_app.outbox.service.mapper.OutboxEventMapper;
import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;
import com.mini.fintech.wallet_app.transfer.repo.TransferTransactionRepository;
import com.mini.fintech.wallet_app.transfer.service.dto.CreateTransferRequestDTO;
import com.mini.fintech.wallet_app.transfer.service.dto.TransferResponseDTO;
import com.mini.fintech.wallet_app.transfer.service.mapper.TransferMapper;
import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;
import com.mini.fintech.wallet_app.wallet.repo.WalletAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
	private final WalletAccountRepository walletAccountRepository;
	private final TransferTransactionRepository transferTransactionRepository;
	private final LedgerService ledgerService;
	private final IdempotencyService idempotencyService;
	private final OutboxService outboxService;
	private final TransferMapper transferMapper;
	private final OutboxEventMapper outboxEventMapper;

	@Value("${wallet.kafka.topics.transfer-completed:fintech.transfer.completed}")
	private String transferCompletedTopic;

	@Transactional
	public TransferResponseDTO createTransfer(CreateTransferRequestDTO request, String idempotencyKey) {
		log.info(
				"Started to create a transfer. Source Wallet ID [{}], Destination Wallet ID [{}], Amount [{}], Currency [{}], and Idempotency Key [{}].",
				request.getSourceWalletId(), request.getDestinationWalletId(), request.getAmount(),
				request.getCurrency(), idempotencyKey);
		try {
			String requestHash = idempotencyService.hashTransferRequest(request);
			log.debug("Computed transfer request hash successfully for Idempotency Key [{}].", idempotencyKey);

			IdempotencyRecord existing = idempotencyService.findRecord(idempotencyKey, TRANSFER_CREATE.name())
					.orElse(null);

			if (existing != null) {
				log.info("Found existing idempotency record. Idempotency Key [{}], Status [{}], and Resource ID [{}].",
						idempotencyKey, existing.getStatus(), existing.getResourceId());
				idempotencyService.validateSameRequest(existing, requestHash);

				if (IdempotencyStatus.COMPLETED.equals(existing.getStatus()) && existing.getResourceId() != null) {
					log.info(
							"Returning previously completed transfer response for Idempotency Key [{}] and Transfer ID [{}].",
							idempotencyKey, existing.getResourceId());
					return loadTransferResponse(UUID.fromString(existing.getResourceId()));
				}

				log.warn("Rejected transfer creation because the same Idempotency Key [{}] is still being processed.",
						idempotencyKey);
				throw new BusinessException(REQUEST_ALREADY_IN_PROGRESS.name(),
						"Another request with this idempotency key is already being processed");
			}

			IdempotencyRecord idempotencyRecord = idempotencyService.createProcessingRecord(idempotencyKey,
					TRANSFER_CREATE.name(), requestHash);
			log.info("Created idempotency processing record. Record ID [{}] and Idempotency Key [{}].",
					idempotencyRecord.getId(), idempotencyRecord.getIdempotencyKey());

			validateDifferentWallets(request.getSourceWalletId(), request.getDestinationWalletId());

			BigDecimal amount = normalizeAmount(request.getAmount());
			String currency = normalizeCurrency(request.getCurrency());

			WalletAccount sourceWallet = loadActiveWallet(request.getSourceWalletId());
			WalletAccount destinationWallet = loadActiveWallet(request.getDestinationWalletId());

			validateCurrency(sourceWallet, currency);
			validateCurrency(destinationWallet, currency);
			ensureSufficientFunds(sourceWallet, amount);

			LocalDateTime now = LocalDateTime.now();
			TransferTransaction transfer = TransferTransaction.builder().id(UUID.randomUUID())
					.sourceWalletId(sourceWallet.getId()).destinationWalletId(destinationWallet.getId()).amount(amount)
					.currency(currency).status(TransferStatus.PENDING).reason(request.getReason()).createdAt(now)
					.build();

			transferTransactionRepository.save(transfer);
			log.info("Saved transfer in pending status. Transfer ID [{}].", transfer.getId());

			sourceWallet.setAvailableBalance(sourceWallet.getAvailableBalance().subtract(amount));
			sourceWallet.setUpdatedAt(now);

			destinationWallet.setAvailableBalance(destinationWallet.getAvailableBalance().add(amount));
			destinationWallet.setUpdatedAt(now);

			walletAccountRepository.save(sourceWallet);
			walletAccountRepository.save(destinationWallet);
			log.info(
					"Updated wallet balances for transfer. Transfer ID [{}], Source Wallet New Balance [{}], Destination Wallet New Balance [{}].",
					transfer.getId(), sourceWallet.getAvailableBalance(), destinationWallet.getAvailableBalance());

			ledgerService.postEntry(transfer.getId(), sourceWallet.getId(), LedgerEntryType.DEBIT, amount, currency,
					LedgerReferenceType.TRANSFER, transfer.getId().toString(),
					defaultString(request.getReason(), "Transfer debit"));

			ledgerService.postEntry(transfer.getId(), destinationWallet.getId(), LedgerEntryType.CREDIT, amount,
					currency, LedgerReferenceType.TRANSFER, transfer.getId().toString(),
					defaultString(request.getReason(), "Transfer credit"));

			transfer.setStatus(TransferStatus.COMPLETED);
			transfer.setCompletedAt(LocalDateTime.now());
			transferTransactionRepository.save(transfer);
			log.info("Marked transfer as completed. Transfer ID [{}] and Completion Time [{}].", transfer.getId(),
					transfer.getCompletedAt());

			outboxService.saveEvent("TRANSFER", transfer.getId().toString(), "transfer.completed",
					transferCompletedTopic, outboxEventMapper.toTransferCompletedEvent(transfer));
			log.info("Saved transfer-completed outbox event for Transfer ID [{}] and Topic [{}].", transfer.getId(),
					transferCompletedTopic);

			TransferResponseDTO response = transferMapper.toResponse(transfer);
			idempotencyService.markCompleted(idempotencyRecord, transfer.getId().toString(), 200, response.toString());
			log.info("Finished transfer successfully. Transfer ID [{}] and Idempotency Key [{}].", transfer.getId(),
					idempotencyKey);
			return response;
		} catch (Exception ex) {
			log.error(
					"Failed to create transfer. Source Wallet ID [{}], Destination Wallet ID [{}], Amount [{}], Currency [{}], and Idempotency Key [{}].",
					request.getSourceWalletId(), request.getDestinationWalletId(), request.getAmount(),
					request.getCurrency(), idempotencyKey, ex);
			throw ex;
		}
	}

	private TransferResponseDTO loadTransferResponse(UUID transferId) {
		log.debug("Loading existing transfer response for Transfer ID [{}].", transferId);
		TransferTransaction transfer = transferTransactionRepository.findById(transferId).orElseThrow(() -> {
			log.warn("Could not find transfer while loading transfer response. Transfer ID [{}].", transferId);
			return new ResourceNotFoundException("Transfer not found: " + transferId);
		});
		return transferMapper.toResponse(transfer);
	}

	private WalletAccount loadActiveWallet(UUID walletId) {
		log.debug("Checking wallet activity status before transfer processing. Wallet ID [{}].", walletId);
		WalletAccount wallet = walletAccountRepository.findById(walletId).orElseThrow(() -> {
			log.warn("Could not find wallet while loading active wallet for transfer flow. Wallet ID [{}].", walletId);
			return new ResourceNotFoundException("Wallet not found: " + walletId);
		});

		if (wallet.getStatus() != WalletStatus.ACTIVE) {
			log.warn("Rejected transfer operation because wallet is not active. Wallet ID [{}] and Status [{}].",
					walletId, wallet.getStatus());
			throw new BusinessException(WALLET_INACTIVE.name(), "Wallet is not active: " + walletId);
		}

		return wallet;
	}

	private void validateDifferentWallets(UUID sourceWalletId, UUID destinationWalletId) {
		if (sourceWalletId.equals(destinationWalletId)) {
			log.warn("Rejected transfer because source and destination wallet IDs are the same. Wallet ID [{}].",
					sourceWalletId);
			throw new BusinessException(INVALID_TRANSFER.name(), "Source and destination wallets must be different");
		}
	}

	private void validateCurrency(WalletAccount wallet, String currency) {
		if (!wallet.getCurrency().equals(currency)) {
			log.warn(
					"Rejected transfer due to currency mismatch. Wallet ID [{}], Wallet Currency [{}], and Transfer Currency [{}].",
					wallet.getId(), wallet.getCurrency(), currency);
			throw new BusinessException(INVALID_CURRENCY.name(), "Transfer currency does not match wallet currency");
		}
	}

	private void ensureSufficientFunds(WalletAccount wallet, BigDecimal amount) {
		if (wallet.getAvailableBalance().compareTo(amount) < 0) {
			log.warn(
					"Rejected transfer due to insufficient funds. Wallet ID [{}], Available Balance [{}], and Requested Amount [{}].",
					wallet.getId(), wallet.getAvailableBalance(), amount);
			throw new BusinessException(INSUFFICIENT_FUNDS.name(), "Source wallet has insufficient balance");
		}
	}
}
