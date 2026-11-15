package com.example.backend.accounts;

import java.util.UUID;

public record AccountRequest(
        String employeeId,
        String firstName,
        String lastName,
        String email,
        String department,
        String jobTitle,
        AccountRole role,
        UUID managerN1Id,
        UUID managerN2Id) {
}
