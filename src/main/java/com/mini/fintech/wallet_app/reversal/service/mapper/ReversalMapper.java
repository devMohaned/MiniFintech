package com.mini.fintech.wallet_app.reversal.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mini.fintech.wallet_app.reversal.domain.ReversalTransaction;
import com.mini.fintech.wallet_app.reversal.service.dto.ReversalResponseDTO;

@Mapper(componentModel = "spring")
public interface ReversalMapper {

	@Mapping(target = "reversalId", source = "id")
	ReversalResponseDTO toResponse(ReversalTransaction reversalTransaction);
}
