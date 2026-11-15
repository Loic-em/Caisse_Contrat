package com.example.backend.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import com.example.backend.cash.AntivirusScanner;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ContractServiceTests {

    private EmploymentContractRepository contracts;
    private UserAccountRepository accounts;
    private ContractService service;

    @BeforeEach
    void setUp() {
        contracts = mock(EmploymentContractRepository.class);
        accounts = mock(UserAccountRepository.class);
        service = new ContractService(
                contracts,
                mock(ContractDocumentRepository.class),
                mock(ContractEventRepository.class),
                accounts,
                mock(AntivirusScanner.class),
                new ContractDocumentCrypto(""));
    }

    @Test
    void administratorCannotReadContracts() {
        UserAccount administrator = activeAccount(AccountRole.ADMINISTRATEUR);
        when(accounts.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(administrator));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.list("admin@example.test"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void accountantCannotEditTheirOwnContract() {
        UUID accountId = UUID.randomUUID();
        UserAccount accountant = activeAccount(AccountRole.COMPTABLE, accountId);
        when(accounts.findByEmailIgnoreCase("accountant@example.test")).thenReturn(Optional.of(accountant));
        EmploymentContract contract = mock(EmploymentContract.class);
        when(contract.getEmployee()).thenReturn(accountant);
        when(contracts.findById(accountId)).thenReturn(Optional.of(contract));
        ContractPayload payload = new ContractPayload(
                accountId, ContractType.CDI, LocalDate.of(2026, 1, 1), null,
                "Comptable", "Finance", null, null, null, null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.update(accountId, payload, "accountant@example.test"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private UserAccount activeAccount(AccountRole role) {
        return activeAccount(role, UUID.randomUUID());
    }

    private UserAccount activeAccount(AccountRole role, UUID id) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(id);
        when(account.getRole()).thenReturn(role);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
        return account;
    }
}
