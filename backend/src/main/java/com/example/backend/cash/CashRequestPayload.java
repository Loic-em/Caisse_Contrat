package com.example.backend.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        String attachmentName,
        @Size(max = 2000) String missionPurpose,
        @Size(max = 120) String missionReference,
        @Size(max = 160) String contactName,
        @Size(max = 50) String contactPhone,
        @Size(max = 160) String activityArea,
        @Size(max = 160) String activityDetails,
        @Size(max = 160) String clientPartner) {

    public CashRequestPayload(
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            String missionPurpose,
            String missionReference,
            String contactName,
            String contactPhone) {
        this(subject, location, startDate, endDate, category, estimatedAmount, attachmentName,
                missionPurpose, missionReference, contactName, contactPhone, null, null, null);
    }

    public CashRequestPayload(
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName) {
        this(subject, location, startDate, endDate, category, estimatedAmount,
                attachmentName, null, null, null, null, null, null, null);
    }
}
