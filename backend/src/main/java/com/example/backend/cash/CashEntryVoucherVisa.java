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
@Table(name = "cash_entry_voucher_visas", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cash_entry_voucher_visa_type", columnNames = { "entry_voucher_id", "visa_type" }),
        @UniqueConstraint(name = "uq_cash_entry_voucher_visa_signer", columnNames = { "entry_voucher_id", "signer_id" })
})
public class CashEntryVoucherVisa {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_voucher_id", nullable = false)
    private CashEntryVoucher entryVoucher;

    @Enumerated(EnumType.STRING)
    @Column(name = "visa_type", nullable = false, length = 20)
    private CashVoucherVisaType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    protected CashEntryVoucherVisa() {
    }

    public CashEntryVoucherVisa(
            UUID id,
            CashEntryVoucher entryVoucher,
            CashVoucherVisaType type,
            UserAccount signer,
            Instant signedAt,
            Instant reauthenticatedAt,
            byte[] signatureSnapshot) {
        this.id = id;
        this.entryVoucher = entryVoucher;
        this.type = type;
        this.signer = signer;
        this.signedAt = signedAt;
        this.reauthenticatedAt = reauthenticatedAt;
        this.signatureSnapshot = signatureSnapshot.clone();
    }

    public UUID getId() { return id; }
    public CashEntryVoucher getEntryVoucher() { return entryVoucher; }
    public CashVoucherVisaType getType() { return type; }
    public UserAccount getSigner() { return signer; }
    public Instant getSignedAt() { return signedAt; }
    public Instant getReauthenticatedAt() { return reauthenticatedAt; }
    public byte[] getSignatureSnapshot() { return signatureSnapshot.clone(); }
}
