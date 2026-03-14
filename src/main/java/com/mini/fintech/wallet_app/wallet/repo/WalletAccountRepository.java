package com.mini.fintech.wallet_app.wallet.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {

	boolean existsByCustomerIdAndCurrency(String customerId, String currency);

	Optional<WalletAccount> findByCustomerIdAndCurrency(String customerId, String currency);
}