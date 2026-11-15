package com.example.backend.contracts;

import com.example.backend.accounts.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employment_contract_documents")
public class ContractDocument {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private EmploymentContract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private UserAccount uploadedBy;

    @Column(nullable = false)
    private int version;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "media_type", nullable = false, length = 32)
    private String mediaType;

    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ContractDocument() {
    }

    public ContractDocument(
            UUID id,
            EmploymentContract contract,
            UserAccount uploadedBy,
            int version,
            String fileName,
            String mediaType,
            byte[] fileData,
            Instant uploadedAt) {
        this.id = id;
        this.contract = contract;
        this.uploadedBy = uploadedBy;
        this.version = version;
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.fileData = fileData;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public EmploymentContract getContract() {
        return contract;
    }

    public UserAccount getUploadedBy() {
        return uploadedBy;
    }

    public int getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
