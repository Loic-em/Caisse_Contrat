package com.example.backend.cash;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CashVoucherPayload(
        @NotBlank @Size(max = 1000) String reason) {
}
