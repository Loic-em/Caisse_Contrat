package com.example.backend.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CashRequestPayload(
        @NotBlank(message = "L’objet de la mission est obligatoire.") String subject,
        @NotBlank(message = "Le lieu de la mission est obligatoire.") String location,
        @NotNull(message = "La date de début est obligatoire.") LocalDate startDate,
        @NotNull(message = "La date de fin est obligatoire.") LocalDate endDate,
        @NotBlank(message = "La catégorie de dépense est obligatoire.") String category,
        @NotNull(message = "Le montant estimé est obligatoire.")
        @DecimalMin(value = "1", message = "Le montant estimé doit être supérieur à zéro.")
        @Digits(integer = 17, fraction = 0, message = "Le montant estimé doit être un nombre entier.") BigDecimal estimatedAmount,
        String attachmentName) {
}
