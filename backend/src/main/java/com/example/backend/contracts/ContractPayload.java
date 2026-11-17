package com.example.backend.contracts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractPayload(
        @NotNull UUID employeeId,
        @NotNull ContractType type,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotBlank @Size(max = 160) String jobTitle,
        @NotBlank @Size(max = 160) String department,
        UUID supervisorId,
        @Size(max = 100) String probationPeriod,
        @Size(max = 200) String workplace,
        @Size(max = 2000) String internalNotes,
        @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 0) BigDecimal baseSalaryAmount,
        @DecimalMin("0") @Digits(integer = 17, fraction = 0) BigDecimal monthlyBonusAmount,
        @Size(max = 2000) String bonusDetails,
        @Size(max = 2000) String otherBenefits) {

    public ContractPayload(
            UUID employeeId,
            ContractType type,
            LocalDate startDate,
            LocalDate endDate,
            String jobTitle,
            String department,
            UUID supervisorId,
            String probationPeriod,
            String workplace,
            String internalNotes) {
        this(employeeId, type, startDate, endDate, jobTitle, department, supervisorId,
                probationPeriod, workplace, internalNotes, BigDecimal.ZERO, null, null, null);
    }
}
