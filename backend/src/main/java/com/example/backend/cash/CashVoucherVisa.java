package com.example.backend.cash;

import com.example.backend.accounts.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_voucher_visas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cash_voucher_visa_type", columnNames = { "voucher_id", "visa_type" }),
        @UniqueConstraint(name = "uq_cash_voucher_visa_signer", columnNames = { "voucher_id", "signer_id" })
})
public class CashVoucherVisa {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private CashVoucher voucher;

    @Enumerated(EnumType.STRING)
    @Column(name = "visa_type", nullable = false, length = 20)
    private CashVoucherVisaType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id", nullable = false)
    private UserAccount signer;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    @Column(name = "reauthenticated_at", nullable = false)
    private Instant reauthenticatedAt;

    @Column(name = "signature_snapshot", nullable = false, columnDefinition = "bytea")
    private byte[] signatureSnapshot;

    @Version
    @Column(nullable = false)
    private Long version;

    protected CashVoucherVisa() {
    }

    public CashVoucherVisa(
            UUID id,
            CashVoucher voucher,
            CashVoucherVisaType type,
            UserAccount signer,
            Instant signedAt,
            Instant reauthenticatedAt,
            byte[] signatureSnapshot) {
        this.id = id;
        this.voucher = voucher;
        this.type = type;
        this.signer = signer;
        this.signedAt = signedAt;
        this.reauthenticatedAt = reauthenticatedAt;
        this.signatureSnapshot = signatureSnapshot.clone();
    }

    public UUID getId() {
        return id;
    }

    public CashVoucher getVoucher() {
        return voucher;
    }

    public CashVoucherVisaType getType() {
        return type;
    }

    public UserAccount getSigner() {
        return signer;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public Instant getReauthenticatedAt() {
        return reauthenticatedAt;
    }

    public byte[] getSignatureSnapshot() {
        return signatureSnapshot.clone();
    }
}
