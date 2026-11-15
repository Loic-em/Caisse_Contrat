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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cash_supporting_documents")
public class SupportingDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CashRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private UserAccount uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaces_id")
    private SupportingDocument replaces;

    @Column(name = "document_type", nullable = false, length = 120)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "media_type", nullable = false, length = 32)
    private String mediaType;

    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(nullable = false)
    private boolean encrypted;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportingDocumentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private UserAccount reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SupportingDocument() {
    }

    public SupportingDocument(
            UUID id,
            CashRequest request,
            UserAccount uploadedBy,
            SupportingDocument replaces,
            String documentType,
            String fileName,
            String mediaType,
            byte[] fileData,
            BigDecimal amount,
            LocalDate documentDate,
            Instant uploadedAt) {
        this.id = id;
        this.request = request;
        this.uploadedBy = uploadedBy;
        this.replaces = replaces;
        this.documentType = documentType;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.fileData = fileData.clone();
        this.encrypted = true;
        this.amount = amount;
        this.documentDate = documentDate;
        this.status = SupportingDocumentStatus.A_CONTROLER;
        this.uploadedAt = uploadedAt;
    }

    public void review(UserAccount reviewer, SupportingDocumentStatus decision, String reason, Instant at) {
        if (status != SupportingDocumentStatus.A_CONTROLER) {
            throw new IllegalArgumentException("Seul un justificatif à contrôler peut être traité.");
        }
        if (decision != SupportingDocumentStatus.ACCEPTE && decision != SupportingDocumentStatus.REJETE) {
            throw new IllegalArgumentException("La décision doit être une acceptation ou un rejet.");
        }
        if (decision == SupportingDocumentStatus.REJETE && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Un motif est obligatoire pour rejeter un justificatif.");
        }
        if (decision == SupportingDocumentStatus.ACCEPTE && reason != null && !reason.isBlank()) {
            throw new IllegalArgumentException("Un motif de rejet ne peut pas accompagner une acceptation.");
        }
        status = decision;
        reviewedBy = reviewer;
        reviewedAt = at;
        rejectionReason = decision == SupportingDocumentStatus.REJETE ? reason.trim() : null;
    }

    public UUID getId() { return id; }
    public CashRequest getRequest() { return request; }
    public UserAccount getUploadedBy() { return uploadedBy; }
    public SupportingDocument getReplaces() { return replaces; }
    public String getDocumentType() { return documentType; }
    public String getFileName() { return fileName; }
    public String getMediaType() { return mediaType; }
    public byte[] getFileData() { return fileData.clone(); }
    public boolean isEncrypted() { return encrypted; }
    public void replaceWithEncryptedData(byte[] encryptedData) {
        this.fileData = encryptedData.clone();
        this.encrypted = true;
    }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDocumentDate() { return documentDate; }
    public SupportingDocumentStatus getStatus() { return status; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getUploadedAt() { return uploadedAt; }
}
