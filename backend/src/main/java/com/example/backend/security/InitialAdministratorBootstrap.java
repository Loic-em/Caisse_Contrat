package com.example.backend.security;

import com.example.backend.accounts.AccountRequest;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountService;
import com.example.backend.accounts.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdministratorBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdministratorBootstrap.class);

    private final AccountService accounts;
    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public InitialAdministratorBootstrap(
            AccountService accounts,
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.accounts = accounts;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            if (!accountRepository.existsByRole(AccountRole.ADMINISTRATEUR)) {
                LOGGER.warn("Aucun administrateur n’existe. Définissez APP_BOOTSTRAP_ADMIN_EMAIL et "
                        + "APP_BOOTSTRAP_ADMIN_PASSWORD, puis redémarrez pour créer le premier compte.");
            }
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_EMAIL et APP_BOOTSTRAP_ADMIN_PASSWORD doivent être définis ensemble.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD doit contenir au moins 12 caractères.");
        }
        if (accountRepository.existsByRole(AccountRole.ADMINISTRATEUR)) {
            return;
        }

        accounts.createInitialAdministratorWithPassword(
                new AccountRequest(
                        "ADMIN-001",
                        "Administrateur",
                        "Initial",
                        email,
                        "Administration",
                        "Administrateur système",
                        AccountRole.ADMINISTRATEUR,
                        null,
                        null),
                passwordEncoder.encode(password));
        LOGGER.info("Le premier compte administrateur a été créé à partir de la configuration d’environnement.");
    }
}
