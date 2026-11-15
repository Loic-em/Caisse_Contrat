package com.example.backend.cash;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_request_events")
public class CashRequestEvent {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @Column(length = 1000)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected CashRequestEvent() {
    }

    public CashRequestEvent(
            UUID id,
            UUID requestId,
            UUID actorId,
            String action,
            String previousStatus,
            String newStatus,
            String reason,
            Instant occurredAt) {
        this.id = id;
        this.requestId = requestId;
        this.actorId = actorId;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }
}
