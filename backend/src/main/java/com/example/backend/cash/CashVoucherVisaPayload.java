package com.example.backend.cash;

import jakarta.validation.constraints.NotNull;

public record CashVoucherVisaPayload(@NotNull CashVoucherVisaType type) {
}
