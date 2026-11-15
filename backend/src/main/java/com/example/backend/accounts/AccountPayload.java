package com.example.backend.accounts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AccountPayload(
        @NotBlank(message = "L’identifiant interne est obligatoire.") String employeeId,
        @NotBlank(message = "Le prénom est obligatoire.") String firstName,
        @NotBlank(message = "Le nom est obligatoire.") String lastName,
        @NotBlank(message = "L’adresse e-mail est obligatoire.") @Email(message = "L’adresse e-mail n’est pas valide.") String email,
        @NotBlank(message = "Le service est obligatoire.") String department,
        @NotBlank(message = "Le poste est obligatoire.") String jobTitle,
        AccountRole role,
        UUID managerN1Id,
        UUID managerN2Id,
        @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères.") String password) {

    public AccountRequest toAccountRequest() {
        return new AccountRequest(
                employeeId,
                firstName,
                lastName,
                email,
                department,
                jobTitle,
                role,
                managerN1Id,
                managerN2Id);
    }
}
