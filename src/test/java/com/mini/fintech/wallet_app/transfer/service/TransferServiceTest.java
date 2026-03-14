package com.mini.fintech.wallet_app.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini.fintech.wallet_app.common.BaseIntegrationTest;
import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.exception.ResourceNotFoundException;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyRecord;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyStatus;
import com.mini.fintech.wallet_app.idemptotency.repo.IdempotencyRecordRepository;
import com.mini.fintech.wallet_app.idemptotency.service.IdempotencyService;
import com.mini.fintech.wallet_app.transfer.domain.TransferStatus;
import com.mini.fintech.wallet_app.transfer.domain.TransferTransaction;
import com.mini.fintech.wallet_app.transfer.repo.TransferTransactionRepository;
import com.mini.fintech.wallet_app.transfer.service.dto.CreateTransferRequestDTO;
import com.mini.fintech.wallet_app.transfer.service.dto.TransferResponseDTO;

class TransferServiceTest extends BaseIntegrationTest {

	@Autowired
	private TransferService transferService;

	@MockitoSpyBean
	private IdempotencyRecordRepository idempotencyRecordRepository;

	@MockitoSpyBean
	private IdempotencyService idempotencyService;

	@MockitoSpyBean
	private TransferTransactionRepository transferTransactionRepository;

	@Test
	void shouldThrowRequestAlreadyInProgressWhenExistingIdempotencyRecordIsStillProcessing() throws Exception {
		CreateTransferRequestDTO request = new CreateTransferRequestDTO();
		request.setSourceWalletId(UUID.randomUUID());
		request.setDestinationWalletId(UUID.randomUUID());
		request.setCurrency("EGP");
		request.setReason("test_reason");
		request.setAmount(new BigDecimal("10.0000"));

		IdempotencyRecord existing = IdempotencyRecord.builder().id(UUID.randomUUID())
				.idempotencyKey("idem-processing-001").status(IdempotencyStatus.PROCESSING)
				.requestHash("DifferentReqHash").build();

		when(idempotencyRecordRepository.findByIdempotencyKeyAndOperationType("idem-processing-001", "TRANSFER_CREATE"))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> transferService.createTransfer(request, "idem-processing-001"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("The same Idempotency-Key cannot be reused with a different request body");

		Mockito.doReturn("DifferentReqHash").when(idempotencyService).hashTransferRequest(request);

		assertThatThrownBy(() -> transferService.createTransfer(request, "idem-processing-001"))
				.isInstanceOf(BusinessException.class).hasMessageContaining("already being processed");
	}

	@Test
	void shouldThrowRequestAlreadyCompletedButResourceIsNullWhenExistingIdempotencyRecordIsStillProcessing()
			throws Exception {
		CreateTransferRequestDTO request = new CreateTransferRequestDTO();
		request.setSourceWalletId(UUID.randomUUID());
		request.setDestinationWalletId(UUID.randomUUID());
		request.setCurrency("EGP");
		request.setReason("test_reason");
		request.setAmount(new BigDecimal("10.0000"));

		IdempotencyRecord existing = IdempotencyRecord.builder().id(UUID.randomUUID())
				.idempotencyKey("idem-processing-001").status(IdempotencyStatus.COMPLETED).resourceId(null)
				.requestHash("Hash").build();

		Mockito.doReturn("Hash").when(idempotencyService).hashTransferRequest(request);

		when(idempotencyRecordRepository.findByIdempotencyKeyAndOperationType("idem-processing-001", "TRANSFER_CREATE"))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> transferService.createTransfer(request, "idem-processing-001"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Another request with this idempotency key is already being processed");
	}

	@Test
	void shouldReturnExistingTransferWhenExistingIdempotencyRecordIsCompleted() throws Exception {
		UUID transferId = UUID.randomUUID();
		String walletId = createWallet("customer_id1", "EGP");
		String walletId2 = createWallet("customer_id2", "EGP");

		CreateTransferRequestDTO request = new CreateTransferRequestDTO();
		request.setSourceWalletId(UUID.fromString(walletId));
		request.setDestinationWalletId(UUID.fromString(walletId2));
		request.setCurrency("EGP");
		request.setReason("test_reason");
		request.setAmount(new BigDecimal("10.0000"));

		IdempotencyRecord existing = IdempotencyRecord.builder().id(UUID.randomUUID())
				.idempotencyKey("idem-completed-001").status(IdempotencyStatus.COMPLETED).requestHash("Hash")
				.resourceId(transferId.toString()).build();

		TransferTransaction transaction = TransferTransaction.builder().id(transferId).status(TransferStatus.COMPLETED)
				.build();

		Mockito.doReturn("Hash").when(idempotencyService).hashTransferRequest(request);

		when(idempotencyRecordRepository.findByIdempotencyKeyAndOperationType("idem-completed-001", "TRANSFER_CREATE"))
				.thenReturn(Optional.of(existing));
		when(transferTransactionRepository.findById(transferId)).thenReturn(Optional.of(transaction));

		TransferResponseDTO response = transferService.createTransfer(request, "idem-completed-001");

		assertThat(response.transferId()).isEqualTo(transferId);
	}

	@Test
	void shouldThrowResourceNotFoundWhenLoadingTransferResponseAndTransferDoesNotExist() {
		UUID transferId = UUID.randomUUID();

		when(transferTransactionRepository.findById(transferId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(transferService, "loadTransferResponse", transferId))
				.isInstanceOf(ResourceNotFoundException.class).hasMessage("Transfer not found: " + transferId);
	}
}
