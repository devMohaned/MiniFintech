package com.mini.fintech.wallet_app.outbox.repo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mini.fintech.wallet_app.outbox.domain.OutboxEvent;
import com.mini.fintech.wallet_app.outbox.domain.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

	List<OutboxEvent> findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(Collection<OutboxStatus> statuses,
			Integer retryMaxCount, Pageable pageable);

	List<OutboxEvent> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Collection<OutboxStatus> statuses,
			LocalDateTime now, Pageable pageable);

}
