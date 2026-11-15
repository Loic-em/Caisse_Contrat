package com.example.backend.cash;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CashVoucherRepository extends JpaRepository<CashVoucher, UUID> {

    Optional<CashVoucher> findByRequestId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select voucher from CashVoucher voucher join fetch voucher.request where voucher.request.id = :requestId")
    Optional<CashVoucher> lockByRequestId(@Param("requestId") UUID requestId);
}
