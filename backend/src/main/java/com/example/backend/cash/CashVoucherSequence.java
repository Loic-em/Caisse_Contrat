package com.example.backend.cash;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cash_voucher_sequences")
public class CashVoucherSequence {

    @Id
    private Long id;

    @Column(name = "next_number", nullable = false)
    private long nextNumber;

    protected CashVoucherSequence() {
    }

    public long reserveNextNumber() {
        if (nextNumber <= 0 || nextNumber == Long.MAX_VALUE) {
            throw new IllegalStateException("La séquence des bons de caisse a atteint sa limite.");
        }
        long reserved = nextNumber;
        nextNumber++;
        return reserved;
    }

    public void setInitialNumber(long initialNumber) {
        if (initialNumber <= 0) {
            throw new IllegalArgumentException("Le premier numéro de bon doit être supérieur à zéro.");
        }
        nextNumber = initialNumber;
    }
}
