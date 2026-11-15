package com.example.backend.cash;

import com.example.backend.accounts.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_return_authorizations")
public class CashReturnAuthorization {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CashRequest request;

    @Column(name = "authorized_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(nullable = false, length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "authorized_by_id", nullable = false)
    private UserAccount authorizedBy;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CashReturnAuthorization() {
    }

    public CashReturnAuthorization(
            UUID id,
            CashRequest request,
            BigDecimal authorizedAmount,
            String reason,
            UserAccount authorizedBy,
            Instant authorizedAt) {
        this.id = id;
        this.request = request;
        this.authorizedAmount = authorizedAmount;
        this.reason = reason.trim();
        this.authorizedBy = authorizedBy;
        this.authorizedAt = authorizedAt;
    }

    public void consume(Instant at) {
        if (consumedAt != null) {
            throw new IllegalArgumentException("Cette autorisation a déjà été utilisée.");
        }
        consumedAt = at;
    }

    public UUID getId() { return id; }
    public CashRequest getRequest() { return request; }
    public BigDecimal getAuthorizedAmount() { return authorizedAmount; }
    public String getReason() { return reason; }
    public UserAccount getAuthorizedBy() { return authorizedBy; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public Instant getConsumedAt() { return consumedAt; }
}
