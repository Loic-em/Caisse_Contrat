package com.example.backend.cash;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupportingDocumentResponse(
        UUID id,
        UUID requestId,
        UUID replacesId,
        String documentType,
        String fileName,
        String mediaType,
        BigDecimal amount,
        LocalDate documentDate,
        SupportingDocumentStatus status,
        String uploadedByName,
        Instant uploadedAt,
        String reviewedByName,
        Instant reviewedAt,
        String rejectionReason) {

    public static SupportingDocumentResponse from(SupportingDocument document) {
        return new SupportingDocumentResponse(
                document.getId(),
                document.getRequest().getId(),
                document.getReplaces() == null ? null : document.getReplaces().getId(),
                document.getDocumentType(),
                document.getFileName(),
                document.getMediaType(),
                document.getAmount(),
                document.getDocumentDate(),
                document.getStatus(),
                fullName(document.getUploadedBy()),
                document.getUploadedAt(),
                document.getReviewedBy() == null ? null : fullName(document.getReviewedBy()),
                document.getReviewedAt(),
                document.getRejectionReason());
    }

    private static String fullName(com.example.backend.accounts.UserAccount account) {
        return account.getFirstName() + " " + account.getLastName();
    }
}
