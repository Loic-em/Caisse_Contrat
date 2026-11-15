package com.example.backend.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashRequestAccountingPayload(
        @NotNull(message = "Le montant définitif est obligatoire.")
        @DecimalMin(value = "1", message = "Le montant définitif doit être supérieur à zéro.")
        @Digits(integer = 17, fraction = 0, message = "Le montant définitif doit être un nombre entier.")
        BigDecimal finalAmount,
        String reason) {
}
