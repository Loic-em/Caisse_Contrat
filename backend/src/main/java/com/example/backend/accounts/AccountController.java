package com.example.backend.accounts;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AccountService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return service.findAll().stream().map(AccountResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AccountResponse create(@Valid @RequestBody AccountPayload payload) {
        requirePassword(payload.password());
        UserAccount account = service.createWithPassword(
                payload.toAccountRequest(),
                passwordEncoder.encode(payload.password()));
        return AccountResponse.from(account);
    }

    @PutMapping("/{id}")
    @Transactional
    public AccountResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody AccountPayload payload,
            Authentication authentication) {
        UserAccount current = service.get(id);
        if (current.getEmail().equalsIgnoreCase(authentication.getName())
                && (current.getRole() != payload.role()
                        || !Objects.equals(managerId(current.getManagerN1()), payload.managerN1Id())
                        || !Objects.equals(managerId(current.getManagerN2()), payload.managerN2Id()))) {
            throw new IllegalArgumentException("Vous ne pouvez pas modifier votre propre rôle ou votre hiérarchie.");
        }
        UserAccount account = service.update(id, payload.toAccountRequest());
        if (payload.password() != null && !payload.password().isBlank()) {
            requirePassword(payload.password());
            account = service.changePassword(id, passwordEncoder.encode(payload.password()));
        }
        return AccountResponse.from(account);
    }

    @PatchMapping("/{id}/deactivate")
    @Transactional
    public AccountResponse deactivate(@PathVariable UUID id, Authentication authentication) {
        UserAccount account = service.get(id);
        if (account.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new IllegalArgumentException("Vous ne pouvez pas désactiver le compte utilisé pour cette session.");
        }
        return AccountResponse.from(service.deactivate(id));
    }

    @PatchMapping("/{id}/reactivate")
    @Transactional
    public AccountResponse reactivate(@PathVariable UUID id) {
        return AccountResponse.from(service.reactivate(id));
    }

    private void requirePassword(String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 12 caractères.");
        }
    }

    private UUID managerId(UserAccount manager) {
        return manager == null ? null : manager.getId();
    }
}
