package com.example.backend.cash;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CashVoucherSequenceRepository extends JpaRepository<CashVoucherSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from CashVoucherSequence sequence where sequence.id = :id")
    Optional<CashVoucherSequence> lockById(@Param("id") Long id);
}
