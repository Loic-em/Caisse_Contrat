package com.example.backend.cash;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashVoucherVisaRepository extends JpaRepository<CashVoucherVisa, UUID> {

    List<CashVoucherVisa> findAllByVoucherIdOrderBySignedAtAsc(UUID voucherId);

    boolean existsByVoucherIdAndType(UUID voucherId, CashVoucherVisaType type);

    boolean existsByVoucherIdAndSignerId(UUID voucherId, UUID signerId);
}
