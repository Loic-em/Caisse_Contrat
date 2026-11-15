package com.example.backend.contracts;

import java.time.Instant;
import java.util.UUID;

public record ContractDocumentResponse(
        UUID id,
        int version,
        String fileName,
        String mediaType,
        Instant uploadedAt,
        String uploadedByName) {

    public static ContractDocumentResponse from(ContractDocument document) {
        return new ContractDocumentResponse(
                document.getId(),
                document.getVersion(),
                document.getFileName(),
                document.getMediaType(),
                document.getUploadedAt(),
                document.getUploadedBy().getFirstName() + " " + document.getUploadedBy().getLastName());
    }
}
