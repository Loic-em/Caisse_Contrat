package com.example.backend.cash;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashEntryVoucherVisaRepository extends JpaRepository<CashEntryVoucherVisa, UUID> {

    boolean existsByEntryVoucherIdAndType(UUID entryVoucherId, CashVoucherVisaType type);

    boolean existsByEntryVoucherIdAndSignerId(UUID entryVoucherId, UUID signerId);
}
