package com.example.backend.security;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.accounts.AccountRequest;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountService;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoAccountBootstrapTests {

    @Test
    void updatesDemoAccountByEmployeeIdWhenItsEmailWasChanged() {
        AccountService accounts = mock(AccountService.class);
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserAccount existing = mock(UserAccount.class);
        UUID accountId = UUID.randomUUID();
        AccountRequest request = new AccountRequest(
                "DEMO-DG",
                "Aminata",
                "Diallo",
                "dg.demo@example.invalid",
                "Direction générale",
                "Directrice générale",
                AccountRole.DIRECTEUR_GENERAL,
                null,
                null);

        when(repository.findByEmployeeId(request.employeeId())).thenReturn(Optional.of(existing));
        when(repository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.empty());
        when(existing.getId()).thenReturn(accountId);
        when(existing.getEmployeeId()).thenReturn("DEMO-DG");
        when(existing.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(accounts.update(accountId, request)).thenReturn(existing);
        when(accounts.changePassword(accountId, "encoded-demo-password")).thenReturn(existing);

        DemoAccountBootstrap bootstrap =
                new DemoAccountBootstrap(accounts, repository, passwordEncoder, true, "demo-password");
        UserAccount result = bootstrap.saveDemoAccount(request, "encoded-demo-password");

        assertSame(existing, result);
        verify(accounts).update(accountId, request);
        verify(accounts).changePassword(accountId, "encoded-demo-password");
        verify(accounts, never()).createWithPassword(request, "encoded-demo-password");
        verify(passwordEncoder, never()).encode("demo-password");
    }

    @Test
    void createsNamedDemoAccountsWithTheSpecifiedManagerChain() {
        AccountService accounts = mock(AccountService.class);
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        String[] employeeIds = {
            "DEMO-DG", "DEMO-CESAR", "DEMO-CARLOS", "DEMO-ADMIN",
            "DEMO-COMPTABLE", "DEMO-ADJOINT", "DEMO-EMPLOYEE", "DEMO-SANDRINE"
        };
        Map<String, UserAccount> users = Arrays.stream(employeeIds)
                .collect(Collectors.toMap(Function.identity(), ignored -> mock(UserAccount.class)));
        UUID cesarId = UUID.randomUUID();
        UUID carlosId = UUID.randomUUID();
        when(users.get("DEMO-CESAR").getId()).thenReturn(cesarId);
        when(users.get("DEMO-CARLOS").getId()).thenReturn(carlosId);
        when(repository.findByEmployeeId(anyString())).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-demo-password");
        when(accounts.createWithPassword(any(AccountRequest.class), anyString()))
                .thenAnswer(invocation -> {
                    AccountRequest request = invocation.getArgument(0);
                    return users.get(request.employeeId());
                });

        DemoAccountBootstrap bootstrap =
                new DemoAccountBootstrap(accounts, repository, passwordEncoder, true, "demo-password-long");
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<AccountRequest> requests = ArgumentCaptor.forClass(AccountRequest.class);
        verify(accounts, times(8)).createWithPassword(requests.capture(), org.mockito.ArgumentMatchers.eq("encoded-demo-password"));
        Map<String, AccountRequest> byEmployeeId = requests.getAllValues().stream()
                .collect(Collectors.toMap(AccountRequest::employeeId, Function.identity()));

        AccountRequest carlos = byEmployeeId.get("DEMO-CARLOS");
        assertEquals(cesarId, carlos.managerN1Id());
        assertEquals("Chef développeur", carlos.jobTitle());

        AccountRequest christophe = byEmployeeId.get("DEMO-EMPLOYEE");
        assertEquals("Christophe", christophe.firstName());
        assertEquals("Designer", christophe.jobTitle());
        assertEquals(carlosId, christophe.managerN1Id());
        assertEquals(cesarId, christophe.managerN2Id());

        AccountRequest sandrine = byEmployeeId.get("DEMO-SANDRINE");
        assertEquals("Sandrine", sandrine.firstName());
        assertEquals("Cheffe de projet", sandrine.jobTitle());
        assertEquals(carlosId, sandrine.managerN1Id());
        assertEquals(cesarId, sandrine.managerN2Id());
    }
}
