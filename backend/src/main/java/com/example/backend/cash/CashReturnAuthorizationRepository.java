package com.example.backend.cash;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashReturnAuthorizationRepository extends JpaRepository<CashReturnAuthorization, UUID> {

    List<CashReturnAuthorization> findAllByRequestIdOrderByAuthorizedAtDesc(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select authorization from CashReturnAuthorization authorization
            join fetch authorization.request
            where authorization.id = :id and authorization.request.id = :requestId
            """)
    Optional<CashReturnAuthorization> lockByIdAndRequestId(
            @Param("id") UUID id,
            @Param("requestId") UUID requestId);
}
