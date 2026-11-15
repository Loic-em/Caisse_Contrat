package com.example.backend.accounts;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String employeeId,
        String firstName,
        String lastName,
        String email,
        String department,
        String jobTitle,
        AccountRole role,
        AccountStatus status,
        UUID managerN1Id,
        String managerN1Name,
        UUID managerN2Id,
        String managerN2Name,
        Instant createdAt,
        Instant deactivatedAt) {

    public static AccountResponse from(UserAccount account) {
        UserAccount managerN1 = account.getManagerN1();
        UserAccount managerN2 = account.getManagerN2();
        return new AccountResponse(
                account.getId(),
                account.getEmployeeId(),
                account.getFirstName(),
                account.getLastName(),
                account.getEmail(),
                account.getDepartment(),
                account.getJobTitle(),
                account.getRole(),
                account.getStatus(),
                managerN1 == null ? null : managerN1.getId(),
                managerN1 == null ? null : managerN1.getFirstName() + " " + managerN1.getLastName(),
                managerN2 == null ? null : managerN2.getId(),
                managerN2 == null ? null : managerN2.getFirstName() + " " + managerN2.getLastName(),
                account.getCreatedAt(),
                account.getDeactivatedAt());
    }
}
