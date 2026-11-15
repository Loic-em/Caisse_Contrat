package com.example.backend.cash;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.example.backend.accounts.AccountRole;

public record CashRequestResponse(
        UUID id,
        String subject,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        String category,
        BigDecimal estimatedAmount,
        String attachmentName,
        CashRequestStatus status,
        UUID requesterId,
        String requesterName,
        String requesterDepartment,
        AccountRole requesterRole,
        UUID validatorN1Id,
        String validatorN1Name,
        UUID validatorN2Id,
        String validatorN2Name,
        BigDecimal finalAmount,
        String amountAdjustmentReason,
        String rejectionReason,
        String cancellationReason,
        Instant submittedAt,
        Instant n1DecisionAt,
        Instant n2DecisionAt,
        Instant createdAt,
        Instant updatedAt) {

    public static CashRequestResponse from(CashRequest request) {
        var requester = request.getRequester();
        var validatorN1 = request.getValidatorN1();
        var validatorN2 = request.getValidatorN2();
        return new CashRequestResponse(
                request.getId(),
                request.getSubject(),
                request.getLocation(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCategory(),
                request.getEstimatedAmount(),
                request.getAttachmentName(),
                request.getStatus(),
                requester == null ? null : requester.getId(),
                requester == null ? null : requester.getFirstName() + " " + requester.getLastName(),
                requester == null ? null : requester.getDepartment(),
                requester == null ? null : requester.getRole(),
                validatorN1 == null ? null : validatorN1.getId(),
                validatorN1 == null ? null : validatorN1.getFirstName() + " " + validatorN1.getLastName(),
                validatorN2 == null ? null : validatorN2.getId(),
                validatorN2 == null ? null : validatorN2.getFirstName() + " " + validatorN2.getLastName(),
                request.getFinalAmount(),
                request.getAmountAdjustmentReason(),
                request.getRejectionReason(),
                request.getCancellationReason(),
                request.getSubmittedAt(),
                request.getN1DecisionAt(),
                request.getN2DecisionAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
