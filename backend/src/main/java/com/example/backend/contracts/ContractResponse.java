package com.example.backend.contracts;

import com.example.backend.accounts.AccountRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        AccountRole employeeRole,
        ContractType type,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String jobTitle,
        String department,
        String probationPeriod,
        String workplace,
        UUID supervisorId,
        String supervisorName,
        String internalNotes,
        int documentCount,
        Instant createdAt,
        Instant updatedAt) {
}
