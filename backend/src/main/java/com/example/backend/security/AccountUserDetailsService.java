package com.example.backend.security;

import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository accounts;

    public AccountUserDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = accounts.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Adresse e-mail ou mot de passe invalide."));
        String encodedPassword = account.getPasswordHash() == null
                ? "{noop}password-not-set"
                : account.getPasswordHash();
        String role = account.getRole() == null ? "USER" : account.getRole().name();

        return User.withUsername(account.getEmail())
                .password(encodedPassword)
                .authorities("ROLE_" + role)
                .disabled(account.getStatus() != com.example.backend.accounts.AccountStatus.ACTIVE
                        || account.getPasswordHash() == null)
                .build();
    }
}
