package com.example.backend.cash;

import java.util.UUID;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface CashRequestRepository extends JpaRepository<CashRequest, UUID> {

    List<CashRequest> findAllByOrderByCreatedAtDesc();

    List<CashRequest> findAllByStatusAndEndDateBefore(CashRequestStatus status, LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from CashRequest request where request.id = :id")
    java.util.Optional<CashRequest> lockById(@Param("id") UUID id);

    @Query("""
            select request from CashRequest request
            where request.requester.id = :accountId
               or request.validatorN1.id = :accountId
               or request.validatorN2.id = :accountId
            order by request.createdAt desc
            """)
    List<CashRequest> findVisibleTo(@Param("accountId") UUID accountId);
}
