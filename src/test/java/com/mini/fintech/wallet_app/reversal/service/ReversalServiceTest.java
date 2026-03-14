package com.mini.fintech.wallet_app.reversal.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.reversal.repo.ReversalTransactionRepository;
import com.mini.fintech.wallet_app.reversal.service.dto.CreateReversalRequestDTO;
import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;
import com.mini.fintech.wallet_app.transfer.repo.TransferTransactionRepository;
import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;
import com.mini.fintech.wallet_app.wallet.repo.WalletAccountRepository;

@ExtendWith(MockitoExtension.class)
class ReversalServiceTest {

	@Mock
	private ReversalTransactionRepository reversalTransactionRepository;

	@Mock
	private TransferTransactionRepository transferTransactionRepository;

	@Mock
	private WalletAccountRepository walletAccountRepository;

	@InjectMocks
	private ReversalService reversalService;

	@Test
	void shouldThrowWhenTransferAlreadyReversed() {
		UUID transferId = UUID.randomUUID();
		TransferTransaction transferTransaction = new TransferTransaction();
		transferTransaction.setStatus(TransferStatus.COMPLETED);
		Mockito.doReturn(Optional.of(transferTransaction)).when(transferTransactionRepository).findById(Mockito.any());
		Mockito.doReturn(true).when(reversalTransactionRepository).existsByOriginalTransferId(transferId);

		CreateReversalRequestDTO createReversalRequestDTO = new CreateReversalRequestDTO();
		createReversalRequestDTO.setReason("Reason");

		assertThatThrownBy(() -> reversalService.reverseTransfer(transferId, createReversalRequestDTO))
				.isInstanceOf(BusinessException.class).hasMessageContaining("already been reversed");
	}

	@Test
	void shouldThrowWhenLoadingActiveWalletForReversalAndWalletNotFound() {
		UUID walletId = UUID.randomUUID();

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(reversalService, "loadActiveWallet", walletId))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Wallet not found");
	}

	@Test
	void shouldThrowWhenLoadingActiveWalletForReversalAndWalletInactive() {
		UUID walletId = UUID.randomUUID();
		WalletAccount wallet = WalletAccount.builder().id(walletId).status(WalletStatus.SUSPENDED).build();

		Mockito.doReturn(Optional.of(wallet)).when(walletAccountRepository).findById(walletId);

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(reversalService, "loadActiveWallet", walletId))
				.isInstanceOf(BusinessException.class).hasMessageContaining("Wallet is not active");
	}
}
