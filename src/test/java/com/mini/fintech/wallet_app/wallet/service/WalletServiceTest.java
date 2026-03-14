package com.mini.fintech.wallet_app.wallet.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.domain.WalletStatus;
import com.mini.fintech.wallet_app.wallet.repo.WalletAccountRepository;
import com.mini.fintech.wallet_app.wallet.service.dto.CreateWalletRequestDTO;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {
	@Mock
	private WalletAccountRepository walletAccountRepository;

	@InjectMocks
	private WalletService walletService;

	@Test
	void shouldRethrowWhenWalletCreationFails() {
		CreateWalletRequestDTO request = new CreateWalletRequestDTO();
		request.setCustomerId("cust_fail_001");
		request.setCurrency("EGP");

		when(walletAccountRepository.existsByCustomerIdAndCurrency("cust_fail_001", "EGP")).thenReturn(false);
		when(walletAccountRepository.save(any(WalletAccount.class)))
				.thenThrow(new RuntimeException("db insert failed"));

		assertThatThrownBy(() -> walletService.createWallet(request)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("db insert failed");
	}

	@Test
	void shouldThrowWhenLoadingActiveWalletAndWalletNotFound() {
		UUID walletId = UUID.randomUUID();
		when(walletAccountRepository.findById(walletId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(walletService, "loadActiveWallet", walletId))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Wallet not found");
	}

	@Test
	void shouldThrowWhenLoadingActiveWalletAndWalletInactive() {
		UUID walletId = UUID.randomUUID();
		WalletAccount wallet = WalletAccount.builder().id(walletId).status(WalletStatus.SUSPENDED) // replace if needed
				.build();

		when(walletAccountRepository.findById(walletId)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(walletService, "loadActiveWallet", walletId))
				.isInstanceOf(BusinessException.class).hasMessageContaining("Wallet is not active");
	}

}