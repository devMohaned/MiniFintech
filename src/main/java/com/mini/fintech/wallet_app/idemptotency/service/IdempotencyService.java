package com.mini.fintech.wallet_app.idemptotency.service;

import static com.mini.fintech.wallet_app.common.util.ErrorCode.IDEMPOTENCY_KEY_REUSED;
import static com.mini.fintech.wallet_app.common.util.ErrorCode.REQUEST_ALREADY_IN_PROGRESS;
import static com.mini.fintech.wallet_app.idemptotency.util.Constants.PIPE_CHARACTER;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.mini.fintech.wallet_app.common.exception.BusinessException;
import com.mini.fintech.wallet_app.common.util.StringUtils;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyRecord;
import com.mini.fintech.wallet_app.idemptotency.domain.IdempotencyStatus;
import com.mini.fintech.wallet_app.idemptotency.repo.IdempotencyRecordRepository;
import com.mini.fintech.wallet_app.transfer.service.dto.CreateTransferRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

	@Value("${wallet.hashing.algorithm}")
	private String hashingAlgorithm;

	@Value("${wallet.hashing.encoding}")
	private String encoding;

	private final IdempotencyRecordRepository idempotencyRecordRepository;

	public String hashTransferRequest(CreateTransferRequestDTO request) {
		log.debug("Started to hash transfer request for Source Wallet ID [{}] and Destination Wallet ID [{}].",
				request.getSourceWalletId(), request.getDestinationWalletId());
		String canonical = request.getSourceWalletId() + PIPE_CHARACTER + request.getDestinationWalletId()
				+ PIPE_CHARACTER + request.getAmount().stripTrailingZeros().toPlainString() + PIPE_CHARACTER
				+ request.getCurrency().trim().toUpperCase() + PIPE_CHARACTER
				+ StringUtils.normalizeNullable(request.getReason());

		try {
			MessageDigest digest = MessageDigest.getInstance(hashingAlgorithm);
			byte[] hash = digest.digest(canonical.getBytes(encoding));
			String hashValue = HexFormat.of().formatHex(hash);
			log.debug("Finished hashing transfer request successfully using Algorithm [{}].", hashingAlgorithm);
			return hashValue;
		} catch (NoSuchAlgorithmException ex) {
			log.error("Failed to hash transfer request because the configured algorithm [{}] is not available.",
					hashingAlgorithm, ex);
			throw new IllegalStateException("%s algorithm not available".formatted(hashingAlgorithm), ex);
		} catch (UnsupportedEncodingException ex) {
			log.error("Failed to hash transfer request because the configured encoding [{}] is not supported.",
					encoding, ex);
			throw new IllegalStateException("%s Encoding is not supported".formatted(encoding), ex);
		}
	}

	public Optional<IdempotencyRecord> findRecord(String idempotencyKey, String operationType) {
		log.debug("Searching for idempotency record with Idempotency Key [{}] and Operation Type [{}].", idempotencyKey,
				operationType);
		return idempotencyRecordRepository.findByIdempotencyKeyAndOperationType(idempotencyKey, operationType);
	}

	public IdempotencyRecord createProcessingRecord(String idempotencyKey, String operationType, String requestHash) {
		log.info("Started to create idempotency processing record for Idempotency Key [{}] and Operation Type [{}].",
				idempotencyKey, operationType);
		LocalDateTime now = LocalDateTime.now();
		IdempotencyRecord record = IdempotencyRecord.builder().id(UUID.randomUUID()).idempotencyKey(idempotencyKey)
				.operationType(operationType).requestHash(requestHash).status(IdempotencyStatus.PROCESSING)
				.createdAt(now).updatedAt(now).build();

		try {
			IdempotencyRecord saved = idempotencyRecordRepository.save(record);
			log.info("Finished creating idempotency processing record successfully. Record ID [{}].", saved.getId());
			return saved;
		} catch (DataIntegrityViolationException ex) {
			log.error(
					"Failed to create idempotency processing record due to duplicate key conflict. Idempotency Key [{}] and Operation Type [{}].",
					idempotencyKey, operationType, ex);
			throw new BusinessException(REQUEST_ALREADY_IN_PROGRESS.name(),
					"Another request with the same idempotency key is already being processed or was completed");
		}
	}

	public void markCompleted(IdempotencyRecord record, String resourceId, int responseCode, String responseBody) {
		log.info(
				"Started to mark idempotency record as completed. Record ID [{}], Resource ID [{}], and Response Code [{}].",
				record.getId(), resourceId, responseCode);
		try {
			record.setResourceId(resourceId);
			record.setResponseCode(responseCode);
			record.setResponseBody(responseBody);
			record.setStatus(IdempotencyStatus.COMPLETED);
			record.setUpdatedAt(LocalDateTime.now());
			idempotencyRecordRepository.save(record);
			log.info("Finished marking idempotency record as completed. Record ID [{}].", record.getId());
		} catch (Exception ex) {
			log.error("Failed to mark idempotency record as completed. Record ID [{}] and Resource ID [{}].",
					record.getId(), resourceId, ex);
			throw ex;
		}
	}

	public void validateSameRequest(IdempotencyRecord record, String requestHash) {
		if (!record.getRequestHash().equals(requestHash)) {
			log.warn("Rejected idempotency request because the request hash does not match. Record ID [{}].",
					record.getId());
			throw new BusinessException(IDEMPOTENCY_KEY_REUSED.name(),
					"The same Idempotency-Key cannot be reused with a different request body");
		}
	}

}
