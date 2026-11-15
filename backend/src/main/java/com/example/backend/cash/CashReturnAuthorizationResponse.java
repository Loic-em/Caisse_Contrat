package com.example.backend.cash;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashReturnAuthorizationResponse(
        UUID id,
        BigDecimal authorizedAmount,
        String reason,
        UUID authorizedById,
        String authorizedByName,
        Instant authorizedAt,
        Instant consumedAt) {

    public static CashReturnAuthorizationResponse from(CashReturnAuthorization authorization) {
        var authorizer = authorization.getAuthorizedBy();
        return new CashReturnAuthorizationResponse(
                authorization.getId(),
                authorization.getAuthorizedAmount(),
                authorization.getReason(),
                authorizer.getId(),
                authorizer.getFirstName() + " " + authorizer.getLastName(),
                authorization.getAuthorizedAt(),
                authorization.getConsumedAt());
    }
}
