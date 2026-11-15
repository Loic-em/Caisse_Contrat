package com.example.backend.security;

import com.example.backend.accounts.AccountRequest;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountService;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DemoAccountBootstrap implements ApplicationRunner {

    private final AccountService accounts;
    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String password;

    public DemoAccountBootstrap(
            AccountService accounts,
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_DEMO_ACCOUNTS_ENABLED:false}") boolean enabled,
            @Value("${APP_DEMO_ACCOUNT_PASSWORD:}") String password) {
        this.accounts = accounts;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            deactivateDemoAccounts();
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "APP_DEMO_ACCOUNT_PASSWORD doit contenir au moins 12 caractères lorsque "
                            + "APP_DEMO_ACCOUNTS_ENABLED=true.");
        }

        String passwordHash = passwordEncoder.encode(password);
        UserAccount director = saveDemoAccount(
                request("DEMO-DG", "Aminata", "Diallo", "dg.demo@example.invalid",
                        "Direction générale", "Directrice générale", AccountRole.DIRECTEUR_GENERAL, null, null),
                passwordHash);
        UserAccount cesar = saveDemoAccount(
                request("DEMO-CESAR", "M.", "César", "cesar.demo@example.invalid",
                        "Direction générale", "N+2", AccountRole.DIRECTEUR_GENERAL, null, null),
                passwordHash);
        UserAccount carlos = saveDemoAccount(
                request("DEMO-CARLOS", "Carlos", "Demo", "carlos.demo@example.invalid",
                        "Développement", "Chef développeur", null, cesar, null),
                passwordHash);
        saveDemoAccount(
                request("DEMO-ADMIN", "Moussa", "Ndiaye", "admin.demo@example.invalid",
                        "Administration", "Administrateur", AccountRole.ADMINISTRATEUR, director, null),
                passwordHash);
        UserAccount accountant = saveDemoAccount(
                request("DEMO-COMPTABLE", "Fatou", "Sow", "comptable.demo@example.invalid",
                        "Comptabilité", "Comptable", AccountRole.COMPTABLE, director, null),
                passwordHash);
        saveDemoAccount(
                request("DEMO-ADJOINT", "Ibrahima", "Fall", "adjoint.demo@example.invalid",
                        "Comptabilité", "Adjoint comptable", AccountRole.ADJOINT_COMPTABLE, accountant, director),
                passwordHash);
        saveDemoAccount(
                request("DEMO-EMPLOYEE", "Christophe", "Demo", "christophe.demo@example.invalid",
                        "Design", "Designer", null, carlos, cesar),
                passwordHash);
        saveDemoAccount(
                request("DEMO-SANDRINE", "Sandrine", "Demo", "sandrine.demo@example.invalid",
                        "Gestion de projet", "Cheffe de projet", null, carlos, cesar),
                passwordHash);
    }

    private void deactivateDemoAccounts() {
        accountRepository.findAll().stream()
                .filter(account -> account.getEmployeeId().startsWith("DEMO-"))
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .forEach(account -> accounts.deactivate(account.getId()));
    }

    UserAccount saveDemoAccount(AccountRequest request, String passwordHash) {
        Optional<UserAccount> byEmployeeId = accountRepository.findByEmployeeId(request.employeeId());
        Optional<UserAccount> byEmail = accountRepository.findByEmailIgnoreCase(request.email());
        if (byEmployeeId.isPresent()
                && byEmail.isPresent()
                && !byEmployeeId.get().getId().equals(byEmail.get().getId())) {
            throw new IllegalStateException(
                    "Un conflit empêche la synchronisation du compte de démonstration "
                            + request.employeeId() + ".");
        }

        UserAccount existing = byEmployeeId.or(() -> byEmail).orElse(null);
        if (existing == null) {
            return accounts.createWithPassword(request, passwordHash);
        }
        if (!existing.getEmployeeId().startsWith("DEMO-")) {
            throw new IllegalStateException(
                    "L’adresse e-mail de démonstration " + request.email()
                            + " est déjà utilisée par un compte non-démonstration.");
        }

        UserAccount account;
        account = accounts.update(existing.getId(), request);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            account = accounts.reactivate(account.getId());
        }
        return accounts.changePassword(account.getId(), passwordHash);
    }

    private AccountRequest request(
            String employeeId,
            String firstName,
            String lastName,
            String email,
            String department,
            String jobTitle,
            AccountRole role,
            UserAccount managerN1,
            UserAccount managerN2) {
        return new AccountRequest(
                employeeId,
                firstName,
                lastName,
                email,
                department,
                jobTitle,
                role,
                managerN1 == null ? null : managerN1.getId(),
                managerN2 == null ? null : managerN2.getId());
    }
}
