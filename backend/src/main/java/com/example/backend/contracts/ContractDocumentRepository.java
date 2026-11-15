package com.example.backend.contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, UUID> {

    List<ContractDocument> findAllByContractIdOrderByVersionDesc(UUID contractId);

    Optional<ContractDocument> findByIdAndContractId(UUID id, UUID contractId);
}
