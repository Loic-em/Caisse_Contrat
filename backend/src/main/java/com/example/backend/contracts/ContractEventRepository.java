package com.example.backend.contracts;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractEventRepository extends JpaRepository<ContractEvent, UUID> {

    List<ContractEvent> findAllByContractIdOrderByOccurredAtDesc(UUID contractId);
}
