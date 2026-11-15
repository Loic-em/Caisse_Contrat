package com.example.backend.cash;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CashDisbursementPayload(
        @NotNull LocalDate disbursedOn,
        @NotNull CashFundOrigin fundOrigin) {
}
