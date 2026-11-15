package com.example.backend.accounts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccountServiceTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserAccountRepository accountRepository;

    @Test
    void createsDirectorGeneralWithoutManagers() {
        UserAccount director = accountService.create(request(
                "DG-001", "DG", "Digin", "dg@digin.example",
                AccountRole.DIRECTEUR_GENERAL, null, null));

        assertNull(director.getManagerN1());
        assertNull(director.getManagerN2());
    }

    @Test
    void createsEmployeeWithDirectorAsN1AndNoN2() {
        UserAccount director = createDirector();

        UserAccount employee = accountService.create(request(
                "EMP-001", "Awa", "Kone", "awa@digin.example",
                null, director.getId(), null));

        assertEquals(director.getId(), employee.getManagerN1().getId());
        assertNull(employee.getManagerN2());
    }

    @Test
    void createsAdministratorWithDirectorAsN1() {
        UserAccount director = createDirector();

        UserAccount administrator = accountService.create(request(
                "ADMIN-001", "Moussa", "Ndiaye", "admin@digin.example",
                AccountRole.ADMINISTRATEUR, director.getId(), null));

        assertEquals(director.getId(), administrator.getManagerN1().getId());
        assertNull(administrator.getManagerN2());
    }

    @Test
    void createsInitialAdministratorBeforeOrganizationalHierarchyExists() {
        UserAccount administrator = accountService.createInitialAdministratorWithPassword(request(
                "ADMIN-001", "Administrateur", "Initial", "admin@digin.example",
                AccountRole.ADMINISTRATEUR, null, null), "hashed-password");

        assertNull(administrator.getManagerN1());
        assertNull(administrator.getManagerN2());
    }

    @Test
    void requiresN2ToMatchTheN1ManagersOwnN1() {
        UserAccount director = createDirector();
        UserAccount manager = accountService.create(request(
                "MGR-001", "Moussa", "Diop", "moussa@digin.example",
                null, director.getId(), null));

        assertThrows(IllegalArgumentException.class, () -> accountService.create(request(
                "EMP-002", "Awa", "Kone", "awa2@digin.example",
                null, manager.getId(), manager.getId())));
    }

    @Test
    void deactivatesWithoutDeletingTheAccount() {
        UserAccount director = createDirector();

        UserAccount deactivated = accountService.deactivate(director.getId());

        assertEquals(AccountStatus.DISABLED, deactivated.getStatus());
        assertEquals(deactivated.getId(), accountService.get(deactivated.getId()).getId());
        assertEquals(1, accountRepository.count());
    }

    @Test
    void rejectsDuplicateEmailWithoutCaseSensitivity() {
        createDirector();

        assertThrows(IllegalArgumentException.class, () -> accountService.create(request(
                "DG-002", "Autre", "Direction", "DG@DIGIN.EXAMPLE",
                AccountRole.DIRECTEUR_GENERAL, null, null)));
    }

    @Test
    void rejectsMissingN1ForNonDirector() {
        assertThrows(IllegalArgumentException.class, () -> accountService.create(request(
                "EMP-003", "Awa", "Kone", "awa3@digin.example",
                null, null, null)));
    }

    @Test
    void rejectsAdminWithoutN1OutsideInitialBootstrap() {
        assertThrows(IllegalArgumentException.class, () -> accountService.create(request(
                "ADMIN-002", "Administrateur", "Autre", "admin2@digin.example",
                AccountRole.ADMINISTRATEUR, null, null)));
    }

    private UserAccount createDirector() {
        return accountService.create(request(
                "DG-001", "DG", "Digin", "dg@digin.example",
                AccountRole.DIRECTEUR_GENERAL, null, null));
    }

    private AccountRequest request(
            String employeeId,
            String firstName,
            String lastName,
            String email,
            AccountRole role,
            UUID managerN1Id,
            UUID managerN2Id) {
        return new AccountRequest(
                employeeId,
                firstName,
                lastName,
                email,
                "Direction",
                "Responsable",
                role,
                managerN1Id,
                managerN2Id);
    }
}
