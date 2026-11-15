package com.example.backend.contracts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_contract_events")
public class ContractEvent {

    @Id
    private UUID id;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(nullable = false, length = 48)
    private String action;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ContractEvent() {
    }

    public ContractEvent(UUID id, UUID contractId, UUID actorId, String action, Instant occurredAt) {
        this.id = id;
        this.contractId = contractId;
        this.actorId = actorId;
        this.action = action;
        this.occurredAt = occurredAt;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
