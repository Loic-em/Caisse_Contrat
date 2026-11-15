package com.example.backend.cash;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashEntryVoucherRepository extends JpaRepository<CashEntryVoucher, UUID> {

    List<CashEntryVoucher> findAllByRequestIdOrderByIssuedAtDesc(UUID requestId);

    long countByRequestId(UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select voucher from CashEntryVoucher voucher where voucher.id = :voucherId and voucher.request.id = :requestId")
    Optional<CashEntryVoucher> lockByIdAndRequestId(
            @Param("voucherId") UUID voucherId, @Param("requestId") UUID requestId);

    long countByRequestIdAndBalanceAfterGreaterThan(UUID requestId, BigDecimal balance);

    @Query("select coalesce(sum(voucher.amount), 0) from CashEntryVoucher voucher where voucher.request.id = :requestId")
    BigDecimal sumAmountByRequestId(@Param("requestId") UUID requestId);
}
