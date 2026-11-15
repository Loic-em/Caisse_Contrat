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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_vouchers")
public class CashVoucher {

    @Id
    private UUID id;

    @Column(name = "voucher_number", nullable = false, unique = true, length = 32)
    private String voucherNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CashVoucherType type;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private CashRequest request;

    @Column(name = "beneficiary_name", nullable = false, length = 241)
    private String beneficiaryName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_in_words", nullable = false, length = 512)
    private String amountInWords;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CashVoucherStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id", nullable = false)
    private UserAccount issuedBy;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<CashVoucherVisa> visas = new ArrayList<>();

    @Column(name = "disbursed_on")
    private LocalDate disbursedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "fund_origin", length = 20)
    private CashFundOrigin fundOrigin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursed_by_id")
    private UserAccount disbursedBy;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected CashVoucher() {
    }

    public CashVoucher(
            UUID id,
            String voucherNumber,
            CashRequest request,
            UserAccount issuedBy,
            String reason,
            Instant issuedAt,
            String amountInWords) {
        this.id = id;
        this.voucherNumber = voucherNumber;
        this.type = CashVoucherType.SORTIE;
        this.request = request;
        this.beneficiaryName = request.getRequester().getFirstName() + " " + request.getRequester().getLastName();
        this.amount = request.getFinalAmount();
        this.amountInWords = amountInWords;
        this.reason = reason.trim();
        this.category = request.getCategory();
        this.status = CashVoucherStatus.EMIS;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
    }

    public void addVisa(CashVoucherVisa visa) {
        this.visas.add(visa);
    }

    public void markDisbursed(
            CashFundOrigin fundOrigin,
            UserAccount disbursedBy,
            LocalDate disbursedOn,
            Instant disbursedAt) {
        if (status != CashVoucherStatus.EMIS) {
            throw new IllegalArgumentException("Ce bon ne peut plus être décaissé.");
        }
        this.status = CashVoucherStatus.DECAISSE;
        this.fundOrigin = fundOrigin;
        this.disbursedBy = disbursedBy;
        this.disbursedOn = disbursedOn;
        this.disbursedAt = disbursedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getVoucherNumber() {
        return voucherNumber;
    }

    public CashVoucherType getType() {
        return type;
    }

    public CashRequest getRequest() {
        return request;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAmountInWords() {
        return amountInWords;
    }

    public String getReason() {
        return reason;
    }

    public String getCategory() {
        return category;
    }

    public CashVoucherStatus getStatus() {
        return status;
    }

    public UserAccount getIssuedBy() {
        return issuedBy;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public List<CashVoucherVisa> getVisas() {
        return List.copyOf(visas);
    }

    public LocalDate getDisbursedOn() {
        return disbursedOn;
    }

    public CashFundOrigin getFundOrigin() {
        return fundOrigin;
    }

    public UserAccount getDisbursedBy() {
        return disbursedBy;
    }

    public Instant getDisbursedAt() {
        return disbursedAt;
    }
}
