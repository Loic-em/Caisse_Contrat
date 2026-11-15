package com.example.backend.security;

import com.example.backend.accounts.AccountResponse;
import com.example.backend.accounts.UserAccountRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/demo-accounts")
@Profile("local")
public class DemoAccountsController {

    private final UserAccountRepository accounts;
    private final boolean enabled;

    public DemoAccountsController(
            UserAccountRepository accounts,
            @Value("${APP_DEMO_ACCOUNTS_ENABLED:false}") boolean enabled) {
        this.accounts = accounts;
        this.enabled = enabled;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return accounts.findAll().stream()
                .filter(account -> account.getEmployeeId().startsWith("DEMO-"))
                .map(AccountResponse::from)
                .toList();
    }
}
