package com.mini.fintech.wallet_app.outbox.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini.fintech.wallet_app.outbox.domain.DeadLetterEvent;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {
}
