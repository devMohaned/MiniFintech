package com.mini.fintech.wallet_app.reversal.service;

import static com.mini.fintech.wallet_app.common.util.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;
import com.mini.fintech.wallet_app.ledger.service.LedgerService;
import com.mini.fintech.wallet_app.outbox.service.OutboxService;
import com.mini.fintech.wallet_app.outbox.service.mapper.OutboxEventMapper;
import com.mini.fintech.wallet_app.reversal.domain.ReversalStatus;
import com.mini.fintech.wallet_app.reversal.domain.ReversalTransaction;
import com.mini.fintech.wallet_app.reversal.repo.ReversalTransactionRepository;
import com.mini.fintech.wallet_app.reversal.service.dto.CreateReversalRequestDTO;
import com.mini.fintech.wallet_app.reversal.service.dto.ReversalResponseDTO;
import com.mini.fintech.wallet_app.reversal.service.mapper.ReversalMapper;
import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;
import com.mini.fintech.wallet_app.transfer.repo.TransferTransactionRepository;
import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;
import com.mini.fintech.wallet_app.wallet.repo.WalletAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReversalService {
	private final TransferTransactionRepository transferTransactionRepository;
	private final ReversalTransactionRepository reversalTransactionRepository;
	private final WalletAccountRepository walletAccountRepository;
	private final LedgerService ledgerService;
	private final OutboxService outboxService;
	private final ReversalMapper reversalMapper;
	private final OutboxEventMapper outboxEventMapper;

	@Value("${wallet.kafka.topics.transfer-reversed:fintech.transfer.reversed}")
	private String transferReversedTopic;

	@Transactional
	public ReversalResponseDTO reverseTransfer(UUID transferId, CreateReversalRequestDTO request) {
		log.info("Started to reverse a transfer. Transfer ID [{}] and Reversal Reason [{}].", transferId,
				request.getReason());
		try {
			TransferTransaction originalTransfer = transferTransactionRepository.findById(transferId)
					.orElseThrow(() -> {
						log.warn("Could not find original transfer while starting reversal flow. Transfer ID [{}].",
								transferId);
						return new ResourceNotFoundException("Transfer not found: " + transferId);
					});

			validateReversible(originalTransfer);

			if (reversalTransactionRepository.existsByOriginalTransferId(transferId)) {
				log.warn("Rejected reversal because transfer was already reversed. Transfer ID [{}].", transferId);
				throw new BusinessException(TRANSFER_ALREADY_REVERSED.name(), "Transfer has already been reversed");
			}

			WalletAccount sourceWallet = loadActiveWallet(originalTransfer.getSourceWalletId());
			WalletAccount destinationWallet = loadActiveWallet(originalTransfer.getDestinationWalletId());

			ensureSufficientFundsForReversal(destinationWallet, originalTransfer);

			LocalDateTime now = LocalDateTime.now();
			ReversalTransaction reversal = ReversalTransaction.builder().id(UUID.randomUUID())
					.originalTransferId(originalTransfer.getId()).status(ReversalStatus.PENDING)
					.reason(request.getReason()).createdAt(now).build();

			reversalTransactionRepository.save(reversal);
			log.info("Created reversal transaction in pending status. Reversal ID [{}] for Transfer ID [{}].",
					reversal.getId(), originalTransfer.getId());

			destinationWallet.setAvailableBalance(
					destinationWallet.getAvailableBalance().subtract(originalTransfer.getAmount()));
			destinationWallet.setUpdatedAt(now);

			sourceWallet.setAvailableBalance(sourceWallet.getAvailableBalance().add(originalTransfer.getAmount()));
			sourceWallet.setUpdatedAt(now);

			walletAccountRepository.save(destinationWallet);
			walletAccountRepository.save(sourceWallet);
			log.info(
					"Updated wallet balances for reversal. Reversal ID [{}], Source Wallet New Balance [{}], and Destination Wallet New Balance [{}].",
					reversal.getId(), sourceWallet.getAvailableBalance(), destinationWallet.getAvailableBalance());

			ledgerService.postEntry(reversal.getId(), destinationWallet.getId(), LedgerEntryType.DEBIT,
					originalTransfer.getAmount(), originalTransfer.getCurrency(), LedgerReferenceType.REVERSAL,
					reversal.getId().toString(), request.getReason());

			ledgerService.postEntry(reversal.getId(), sourceWallet.getId(), LedgerEntryType.CREDIT,
					originalTransfer.getAmount(), originalTransfer.getCurrency(), LedgerReferenceType.REVERSAL,
					reversal.getId().toString(), request.getReason());

			now = LocalDateTime.now();
			originalTransfer.setStatus(TransferStatus.REVERSED);
			originalTransfer.setReversedAt(now);
			transferTransactionRepository.save(originalTransfer);
			log.info("Marked original transfer as reversed. Transfer ID [{}] and Reversed At [{}].",
					originalTransfer.getId(), originalTransfer.getReversedAt());

			reversal.setStatus(ReversalStatus.COMPLETED);
			reversal.setCompletedAt(now);
			reversalTransactionRepository.save(reversal);
			log.info(
					"Finished transfer reversal transaction update. Reversal ID [{}], Transfer ID [{}], and Completion Time [{}].",
					reversal.getId(), originalTransfer.getId(), reversal.getCompletedAt());

			outboxService.saveEvent("REVERSAL", reversal.getId().toString(), "transfer.reversed", transferReversedTopic,
					outboxEventMapper.toTransferReversedEvent(reversal, originalTransfer));
			log.info("Saved transfer-reversed outbox event for Reversal ID [{}] and Topic [{}].", reversal.getId(),
					transferReversedTopic);

			log.info("Finished transfer reversal successfully. Reversal ID [{}] and Transfer ID [{}].",
					reversal.getId(), originalTransfer.getId());
			return reversalMapper.toResponse(reversal);
		} catch (Exception ex) {
			log.error("Failed to reverse transfer. Transfer ID [{}] and Reversal Reason [{}].", transferId,
					request.getReason(), ex);
			throw ex;
		}
	}

	private void validateReversible(TransferTransaction transfer) {
		if (!TransferStatus.COMPLETED.equals(transfer.getStatus())) {
			log.warn(
					"Rejected reversal because transfer is not in completed state. Transfer ID [{}] and Current Status [{}].",
					transfer.getId(), transfer.getStatus());
			throw new BusinessException(TRANSFER_NOT_REVERSIBLE.name(), "Only completed transfers can be reversed");
		}
	}

	private WalletAccount loadActiveWallet(UUID walletId) {
		log.debug("Checking wallet activity status before reversal processing. Wallet ID [{}].", walletId);
		WalletAccount wallet = walletAccountRepository.findById(walletId).orElseThrow(() -> {
			log.warn("Could not find wallet while loading active wallet for reversal flow. Wallet ID [{}].", walletId);
			return new ResourceNotFoundException("Wallet not found: " + walletId);
		});

		if (!WalletStatus.ACTIVE.equals(wallet.getStatus())) {
			log.warn("Rejected reversal operation because wallet is not active. Wallet ID [{}] and Status [{}].",
					walletId, wallet.getStatus());
			throw new BusinessException(WALLET_INACTIVE.name(), "Wallet is not active: " + walletId);
		}

		return wallet;
	}

	private void ensureSufficientFundsForReversal(WalletAccount destinationWallet, TransferTransaction transfer) {
		if (destinationWallet.getAvailableBalance().compareTo(transfer.getAmount()) < 0) {
			log.warn(
					"Rejected reversal due to insufficient funds in destination wallet. Destination Wallet ID [{}], Available Balance [{}], and Required Amount [{}].",
					destinationWallet.getId(), destinationWallet.getAvailableBalance(), transfer.getAmount());
			throw new BusinessException(REVERSAL_INSUFFICIENT_FUNDS.name(),
					"Destination wallet does not have enough balance to reverse this transfer");
		}
	}

}
