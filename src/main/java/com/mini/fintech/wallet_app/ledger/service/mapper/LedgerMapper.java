package com.mini.fintech.wallet_app.ledger.service.mapper;

import org.mapstruct.Mapper;

import com.mini.fintech.wallet_app.ledger.domain.LedgerEntry;
import com.mini.fintech.wallet_app.ledger.service.dto.LedgerEntryResponseDTO;

@Mapper(componentModel = "spring")
public interface LedgerMapper {

	LedgerEntryResponseDTO toResponse(LedgerEntry ledgerEntry);
}
