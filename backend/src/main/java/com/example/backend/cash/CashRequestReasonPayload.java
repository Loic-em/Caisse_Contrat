package com.example.backend.cash;

import jakarta.validation.constraints.NotBlank;

public record CashRequestReasonPayload(
        @NotBlank(message = "Un motif est obligatoire.") String reason) {
}
