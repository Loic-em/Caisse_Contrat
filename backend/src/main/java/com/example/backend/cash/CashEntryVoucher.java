package com.example.backend.cash;

import com.example.backend.accounts.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_entry_vouchers")
public class CashEntryVoucher {

    @Id
    private UUID id;

    @Column(name = "voucher_number", nullable = false, unique = true, length = 32)
    private String voucherNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CashRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_voucher_id", nullable = false)
    private CashVoucher sourceVoucher;

    @Column(name = "beneficiary_name", nullable = false, length = 241)
    private String beneficiaryName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_in_words", nullable = false, length = 512)
    private String amountInWords;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "returned_on", nullable = false)
    private LocalDate returnedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "fund_origin", nullable = false, length = 20)
    private CashFundOrigin fundOrigin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by_id", nullable = false)
    private UserAccount issuedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id", unique = true)
    private CashReturnAuthorization authorization;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @OneToMany(mappedBy = "entryVoucher", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<CashEntryVoucherVisa> visas = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected CashEntryVoucher() {
    }

    public CashEntryVoucher(
            UUID id,
            String voucherNumber,
            CashRequest request,
            CashVoucher sourceVoucher,
            UserAccount issuedBy,
            CashReturnAuthorization authorization,
            BigDecimal amount,
            String reason,
            LocalDate returnedOn,
            CashFundOrigin fundOrigin,
            Instant issuedAt,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter) {
        this.id = id;
        this.voucherNumber = voucherNumber;
        this.request = request;
        this.sourceVoucher = sourceVoucher;
        this.beneficiaryName = sourceVoucher.getBeneficiaryName();
        this.amount = amount;
        this.amountInWords = FrenchAmountInWords.format(amount);
        this.reason = reason.trim();
        this.returnedOn = returnedOn;
        this.fundOrigin = fundOrigin;
        this.issuedBy = issuedBy;
        this.authorization = authorization;
        this.issuedAt = issuedAt;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
    }

    public UUID getId() { return id; }
    public String getVoucherNumber() { return voucherNumber; }
    public CashRequest getRequest() { return request; }
    public CashVoucher getSourceVoucher() { return sourceVoucher; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public BigDecimal getAmount() { return amount; }
    public String getAmountInWords() { return amountInWords; }
    public String getReason() { return reason; }
    public LocalDate getReturnedOn() { return returnedOn; }
    public UserAccount getIssuedBy() { return issuedBy; }
    public CashReturnAuthorization getAuthorization() { return authorization; }
    public Instant getIssuedAt() { return issuedAt; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public CashFundOrigin getFundOrigin() { return fundOrigin; }
    public List<CashEntryVoucherVisa> getVisas() { return List.copyOf(visas); }
    public void addVisa(CashEntryVoucherVisa visa) { visas.add(visa); }
}
