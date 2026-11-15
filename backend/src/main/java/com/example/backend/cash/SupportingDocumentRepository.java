package com.example.backend.cash;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportingDocumentRepository extends JpaRepository<SupportingDocument, UUID> {

    List<SupportingDocument> findAllByRequestIdOrderByUploadedAtDesc(UUID requestId);

    Optional<SupportingDocument> findByIdAndRequestId(UUID id, UUID requestId);

    boolean existsByRequestIdAndStatus(UUID requestId, SupportingDocumentStatus status);

    List<SupportingDocument> findTop10ByEncryptedFalseOrderByUploadedAtAsc();

    @Query("""
            select coalesce(sum(document.amount), 0) from SupportingDocument document
            where document.request.id = :requestId and document.status = :status
            """)
    java.math.BigDecimal sumAmountByRequestIdAndStatus(
            @Param("requestId") UUID requestId,
            @Param("status") SupportingDocumentStatus status);
}
