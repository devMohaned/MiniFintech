package com.mini.fintech.wallet_app.wallet.service.mapper;

import org.mapstruct.Mapper;

import com.mini.fintech.wallet_app.wallet.domain.WalletAccount;
import com.mini.fintech.wallet_app.wallet.service.dto.WalletResponseDTO;

@Mapper(componentModel = "spring")
public interface WalletMapper {

	WalletResponseDTO toResponse(WalletAccount walletAccount);
}
