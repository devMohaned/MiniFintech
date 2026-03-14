package com.mini.fintech.wallet_app.outbox.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mini.fintech.wallet_app.outbox.service.dto.TransferCompletedEventDTO;
import com.mini.fintech.wallet_app.outbox.service.dto.TransferReversedEventDTO;
import com.mini.fintech.wallet_app.reversal.domain.ReversalTransaction;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;

@Mapper(componentModel = "spring")
public interface OutboxEventMapper {

	@Mapping(target = "transferId", source = "id")
	@Mapping(target = "occurredAt", source = "completedAt")
	TransferCompletedEventDTO toTransferCompletedEvent(TransferTransaction transferTransaction);

	@Mapping(target = "reversalId", source = "reversal.id")
	@Mapping(target = "originalTransferId", source = "originalTransfer.id")
	@Mapping(target = "sourceWalletId", source = "originalTransfer.sourceWalletId")
	@Mapping(target = "destinationWalletId", source = "originalTransfer.destinationWalletId")
	@Mapping(target = "amount", source = "originalTransfer.amount")
	@Mapping(target = "currency", source = "originalTransfer.currency")
	@Mapping(target = "reason", source = "reversal.reason")
	@Mapping(target = "occurredAt", source = "reversal.completedAt")
	TransferReversedEventDTO toTransferReversedEvent(ReversalTransaction reversal,
			TransferTransaction originalTransfer);
}
