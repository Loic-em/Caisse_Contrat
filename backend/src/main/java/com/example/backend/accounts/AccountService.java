package com.example.backend.accounts;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserAccountRepository accounts;
    private final Clock clock;

    @Autowired
    public AccountService(UserAccountRepository accounts) {
        this(accounts, Clock.systemUTC());
    }

    AccountService(UserAccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    public UserAccount create(AccountRequest request) {
        validateRequiredFields(request);
        return createValidated(request, resolveHierarchy(request, null));
    }

    @Transactional
    public UserAccount createInitialAdministratorWithPassword(AccountRequest request, String passwordHash) {
        validateRequiredFields(request);
        if (request.role() != AccountRole.ADMINISTRATEUR
                || request.managerN1Id() != null
                || request.managerN2Id() != null) {
            throw new IllegalArgumentException("Le compte initial doit être un administrateur sans hiérarchie.");
        }
        UserAccount account = createValidated(request, new Hierarchy(null, null));
        account.setPasswordHash(passwordHash);
        return accounts.save(account);
    }

    private UserAccount createValidated(AccountRequest request, Hierarchy hierarchy) {
        String employeeId = request.employeeId().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Cet identifiant interne est déjà utilisé.");
        }
        if (accounts.existsByEmail(email)) {
            throw new IllegalArgumentException("Cette adresse e-mail est déjà utilisée.");
        }
        UserAccount account = new UserAccount(
                UUID.randomUUID(),
                employeeId,
                request.firstName(),
                request.lastName(),
                email,
                request.department(),
                request.jobTitle(),
                request.role(),
                hierarchy.managerN1(),
                hierarchy.managerN2(),
                clock.instant());
        return accounts.save(account);
    }

    @Transactional
    public UserAccount createWithPassword(AccountRequest request, String passwordHash) {
        UserAccount account = create(request);
        account.setPasswordHash(passwordHash);
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public List<UserAccount> findAll() {
        return accounts.findAll();
    }

    @Transactional
    public UserAccount update(UUID accountId, AccountRequest request) {
        validateRequiredFields(request);
        UserAccount account = findAccount(accountId);
        String employeeId = request.employeeId().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmployeeIdAndIdNot(employeeId, accountId)) {
            throw new IllegalArgumentException("Cet identifiant interne est déjà utilisé.");
        }
        if (accounts.existsByEmailAndIdNot(email, accountId)) {
            throw new IllegalArgumentException("Cette adresse e-mail est déjà utilisée.");
        }
        Hierarchy hierarchy = resolveHierarchy(request, accountId);
        account.updateDetails(
                employeeId,
                request.firstName(),
                request.lastName(),
                email,
                request.department(),
                request.jobTitle(),
                request.role(),
                hierarchy.managerN1(),
                hierarchy.managerN2());
        return accounts.save(account);
    }

    @Transactional
    public UserAccount deactivate(UUID accountId) {
        UserAccount account = findAccount(accountId);
        account.deactivate(clock.instant());
        return accounts.save(account);
    }

    @Transactional
    public UserAccount reactivate(UUID accountId) {
        UserAccount account = findAccount(accountId);
        account.reactivate();
        return accounts.save(account);
    }

    @Transactional
    public UserAccount changePassword(UUID accountId, String passwordHash) {
        UserAccount account = findAccount(accountId);
        account.setPasswordHash(passwordHash);
        return accounts.save(account);
    }

    @Transactional(readOnly = true)
    public UserAccount get(UUID accountId) {
        return findAccount(accountId);
    }

    private UserAccount findAccount(UUID accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Aucun compte ne correspond à l’identifiant " + accountId + "."));
    }

    private void validateRequiredFields(AccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Les informations du compte sont obligatoires.");
        }
        requireText(request.employeeId(), "L’identifiant interne");
        requireText(request.firstName(), "Le prénom");
        requireText(request.lastName(), "Le nom");
        requireText(request.email(), "L’adresse e-mail");
        requireText(request.department(), "Le service");
        requireText(request.jobTitle(), "Le poste");
        if (!request.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("L’adresse e-mail n’est pas valide.");
        }
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " est obligatoire.");
        }
    }

    private Hierarchy resolveHierarchy(AccountRequest request, UUID accountId) {
        if (request.role() == AccountRole.ADMINISTRATEUR) {
            return new Hierarchy(null, null);
        }
        if (request.managerN1Id() == null) {
            if (request.role() == AccountRole.DIRECTEUR_GENERAL) {
                return new Hierarchy(null, null);
            }
            throw new IllegalArgumentException("Le responsable N+1 est obligatoire.");
        }
        UserAccount managerN1 = findActiveManager(request.managerN1Id(), "N+1");
        if (managerN1.getId().equals(accountId)) {
            throw new IllegalArgumentException("Un compte ne peut pas etre son propre superieur.");
        }

        if (managerN1.getRole() == AccountRole.DIRECTEUR_GENERAL) {
            if (request.managerN2Id() != null) {
                throw new IllegalArgumentException("Un compte rattache au directeur general ne peut pas avoir de N+2.");
            }
            return new Hierarchy(managerN1, null);
        }

        if (request.managerN2Id() == null) {
            throw new IllegalArgumentException("Le responsable N+2 est obligatoire.");
        }
        UserAccount managerN2 = findActiveManager(request.managerN2Id(), "N+2");
        if (managerN2.getId().equals(accountId) || managerN2.getId().equals(managerN1.getId())) {
            throw new IllegalArgumentException("La hierarchie selectionnee n'est pas valide.");
        }
        if (managerN1.getManagerN1() == null
                || !managerN1.getManagerN1().getId().equals(managerN2.getId())) {
            throw new IllegalArgumentException("Le N+2 doit etre le N+1 du responsable N+1.");
        }
        return new Hierarchy(managerN1, managerN2);
    }

    private UserAccount findActiveManager(UUID managerId, String level) {
        UserAccount manager = accounts.findById(managerId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Le responsable " + level + " sélectionné n’existe pas."));
        if (manager.getStatus() != AccountStatus.ACTIVE || manager.getRole() == AccountRole.ADMINISTRATEUR) {
            throw new IllegalArgumentException("Le responsable selectionne n'est pas un compte actif autorise.");
        }
        return manager;
    }

    private record Hierarchy(UserAccount managerN1, UserAccount managerN2) {
    }
}
