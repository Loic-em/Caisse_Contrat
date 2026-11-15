package com.example.backend.cash;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CashRegularizationResponse(
        UUID requestId,
        String sourceVoucherNumber,
        BigDecimal disbursedAmount,
        BigDecimal acceptedDocumentAmount,
        BigDecimal returnedAmount,
        BigDecimal outstandingBalance,
        long partialReturnCount,
        List<CashEntryVoucherResponse> entryVouchers,
        List<CashReturnAuthorizationResponse> authorizations) {
}
