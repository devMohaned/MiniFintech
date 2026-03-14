package com.mini.fintech.wallet_app.ledger.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.ledger.domain.LedgerEntry;
import com.mini.fintech.wallet_app.ledger.domain.LedgerEntryType;
import com.mini.fintech.wallet_app.ledger.domain.LedgerReferenceType;
import com.mini.fintech.wallet_app.ledger.repo.LedgerEntryRepository;
import com.mini.fintech.wallet_app.ledger.service.dto.LedgerEntryResponseDTO;
import com.mini.fintech.wallet_app.ledger.service.mapper.LedgerMapper;
import com.mini.fintech.wallet_app.wallet.repo.WalletAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

	private final LedgerEntryRepository ledgerEntryRepository;
	private final WalletAccountRepository walletAccountRepository;
	private final LedgerMapper ledgerMapper;

	public void postEntry(UUID transactionId, UUID walletId, LedgerEntryType entryType, BigDecimal amount,
			String currency, LedgerReferenceType referenceType, String referenceId, String description) {
		log.info(
				"Started to post a ledger entry. Transaction ID [{}], Wallet ID [{}], Entry Type [{}], Amount [{}], Currency [{}], and Reference ID [{}].",
				transactionId, walletId, entryType, amount, currency, referenceId);
		try {
			LedgerEntry entry = LedgerEntry.builder().id(UUID.randomUUID()).transactionId(transactionId)
					.walletId(walletId).entryType(entryType).amount(amount).currency(currency)
					.referenceType(referenceType).referenceId(referenceId).description(description)
					.createdAt(LocalDateTime.now()).build();

			ledgerEntryRepository.save(entry);
			log.info("Finished posting ledger entry successfully. Ledger Entry ID [{}] for Transaction ID [{}].",
					entry.getId(), transactionId);
		} catch (Exception ex) {
			log.error(
					"Failed to post ledger entry. Transaction ID [{}], Wallet ID [{}], Entry Type [{}], Amount [{}], and Currency [{}].",
					transactionId, walletId, entryType, amount, currency, ex);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public Page<LedgerEntryResponseDTO> getWalletLedger(UUID walletId, Pageable pageable) {
		log.info("Started to fetch ledger entries for Wallet ID [{}] with Page Number [{}] and Page Size [{}].",
				walletId, pageable.getPageNumber(), pageable.getPageSize());
		try {
			if (!walletAccountRepository.existsById(walletId)) {
				log.warn("Cannot fetch ledger entries because wallet does not exist. Wallet ID [{}].", walletId);
				throw new ResourceNotFoundException("Wallet not found: " + walletId);
			}

			Page<LedgerEntryResponseDTO> page = ledgerEntryRepository
					.findByWalletIdOrderByCreatedAtDesc(walletId, pageable).map(ledgerMapper::toResponse);
			log.info("Finished fetching ledger entries for Wallet ID [{}]. Retrieved [{}] records on this page.",
					walletId, page.getNumberOfElements());
			return page;
		} catch (Exception ex) {
			log.error("Failed to fetch ledger entries for Wallet ID [{}].", walletId, ex);
			throw ex;
		}
	}
}
