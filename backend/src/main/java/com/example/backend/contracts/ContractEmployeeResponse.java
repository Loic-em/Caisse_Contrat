package com.example.backend.contracts;

import com.example.backend.accounts.UserAccount;
import java.util.UUID;

public record ContractEmployeeResponse(
        UUID id,
        String employeeId,
        String firstName,
        String lastName,
        String department,
        String jobTitle,
        UUID managerN1Id,
        String managerN1Name) {

    public static ContractEmployeeResponse from(UserAccount account) {
        UserAccount manager = account.getManagerN1();
        return new ContractEmployeeResponse(
                account.getId(),
                account.getEmployeeId(),
                account.getFirstName(),
                account.getLastName(),
                account.getDepartment(),
                account.getJobTitle(),
                manager == null ? null : manager.getId(),
                manager == null ? null : manager.getFirstName() + " " + manager.getLastName());
    }
}
