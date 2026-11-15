package com.example.backend.cash;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashRequestEventRepository extends JpaRepository<CashRequestEvent, UUID> {

    List<CashRequestEvent> findByRequestIdOrderByOccurredAtDesc(UUID requestId);
}
