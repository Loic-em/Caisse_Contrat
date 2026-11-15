package com.example.backend.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_signatures")
public class UserSignature {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "signature_png", nullable = false, columnDefinition = "bytea")
    private byte[] signaturePng;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSignature() {
    }

    public UserSignature(UUID userId, byte[] signaturePng, Instant updatedAt) {
        this.userId = userId;
        this.signaturePng = signaturePng.clone();
        this.updatedAt = updatedAt;
    }

    public byte[] getSignaturePng() {
        return signaturePng.clone();
    }

    public void update(byte[] signaturePng, Instant updatedAt) {
        this.signaturePng = signaturePng.clone();
        this.updatedAt = updatedAt;
    }
}
