package com.mini.fintech.wallet_app.transfer.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;
import com.mini.fintech.wallet_app.transfer.service.dto.TransferResponseDTO;

@Mapper(componentModel = "spring")
public interface TransferMapper {

	@Mapping(target = "transferId", source = "id")
	TransferResponseDTO toResponse(TransferTransaction transferTransaction);
}
