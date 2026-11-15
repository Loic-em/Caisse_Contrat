package com.example.backend.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @Size(max = 2000) String internalNotes) {
}
