package com.example.backend.cash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.backend.accounts.AccountRequest;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountService;
import com.example.backend.accounts.UserAccount;
import com.example.backend.security.UserSignature;
import com.example.backend.security.UserSignatureRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class CashRequestControllerTests {

    @Autowired
    private CashRequestService cashRequestService;

    @Autowired
    private CashRequestRepository cashRequestRepository;

    @Autowired
    private CashRequestEventRepository eventRepository;

    @Autowired
    private CashVoucherService cashVoucherService;

    @Autowired
    private CashVoucherRepository cashVoucherRepository;

    @Autowired
    private CashVoucherVisaRepository cashVoucherVisaRepository;

    @Autowired
    private UserSignatureRepository userSignatureRepository;

    @Autowired
    private AccountService accountService;

    @Test
    void createsCashRequestAsDraftOwnedByTheAuthenticatedUser() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount requester = createEmployee("EMP-001", "awa", director, null);

        CashRequestResponse created = cashRequestService.create(payload(
                "Mission partenaires", new BigDecimal("250000")), requester.getEmail());

        assertNotNull(created.id());
        assertEquals("Mission partenaires", created.subject());
        assertEquals(CashRequestStatus.DRAFT, created.status());
        assertEquals(requester.getId(), created.requesterId());
        assertEquals(new BigDecimal("250000"), created.estimatedAmount());
        assertEquals(1, cashRequestRepository.count());
        assertEquals(1, eventRepository.count());
    }

    @Test
    void routesRequestThroughBothManagersBeforeAccountingReview() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount manager = createEmployee("MGR-001", "manager1", director, null);
        UserAccount requester = createEmployee("EMP-001", "employee1", manager, director);
        UserAccount accountant = createAccountant("ACC-001", "accountant1", director);

        CashRequestResponse created = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());
        CashRequestResponse submitted = cashRequestService.submit(created.id(), requester.getEmail());
        CashRequestResponse firstApproval = cashRequestService.approve(submitted.id(), manager.getEmail());
        CashRequestResponse secondApproval = cashRequestService.approve(firstApproval.id(), director.getEmail());
        CashRequestResponse amountSet = cashRequestService.setFinalAmount(
                secondApproval.id(), new BigDecimal("240000"), "Estimation ajustée", accountant.getEmail());

        assertEquals(CashRequestStatus.SOUMISE, submitted.status());
        assertEquals(CashRequestStatus.VALIDEE_N1, firstApproval.status());
        assertEquals(CashRequestStatus.VALIDEE, secondApproval.status());
        assertEquals(CashRequestStatus.MONTANT_ARRETE, amountSet.status());
        assertEquals(new BigDecimal("240000"), amountSet.finalAmount());
        assertEquals(5, eventRepository.count());
    }

    @Test
    void routesDirectlyToAccountingWhenDirectorGeneralIsTheN1() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount requester = createEmployee("EMP-001", "employee1", director, null);

        CashRequestResponse created = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());
        CashRequestResponse submitted = cashRequestService.submit(created.id(), requester.getEmail());
        CashRequestResponse approved = cashRequestService.approve(submitted.id(), director.getEmail());

        assertEquals(CashRequestStatus.VALIDEE, approved.status());
        assertEquals(director.getId(), approved.validatorN1Id());
        assertNull(approved.validatorN2Id());
    }

    @Test
    void rejectsSelfApprovalAndRequestsOutsideTheViewersPerimeter() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount manager = createEmployee("MGR-001", "manager1", director, null);
        UserAccount requester = createEmployee("EMP-001", "employee1", manager, director);
        UserAccount otherEmployee = createEmployee("EMP-002", "employee2", manager, director);

        CashRequestResponse created = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());
        CashRequestResponse submitted = cashRequestService.submit(created.id(), requester.getEmail());

        assertThrows(ResponseStatusException.class,
                () -> cashRequestService.approve(submitted.id(), requester.getEmail()));
        assertEquals(0, cashRequestService.findAll(otherEmployee.getEmail()).size());
        assertEquals(1, cashRequestService.findAll(manager.getEmail()).size());
    }

    @Test
    void rejectsCashRequestWithEndDateBeforeStartDate() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount requester = createEmployee("EMP-001", "awa", director, null);
        CashRequestPayload invalidPayload = new CashRequestPayload(
                "Mission partenaires",
                "Dakar",
                LocalDate.of(2026, 11, 25),
                LocalDate.of(2026, 11, 20),
                "Transport",
                new BigDecimal("250000"),
                null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cashRequestService.create(invalidPayload, requester.getEmail()));

        assertEquals("La date de fin doit être supérieure ou égale à la date de début.", exception.getMessage());
    }

    @Test
    void emitsNumberedVoucherOnlyAfterAccountingHasFixedTheAmount() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount manager = createEmployee("MGR-001", "manager1", director, null);
        UserAccount requester = createEmployee("EMP-001", "employee1", manager, director);
        UserAccount accountant = createAccountant("ACC-001", "accountant1", director);

        CashRequestResponse created = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());
        CashRequestResponse submitted = cashRequestService.submit(created.id(), requester.getEmail());
        CashRequestResponse firstApproval = cashRequestService.approve(submitted.id(), manager.getEmail());
        CashRequestResponse secondApproval = cashRequestService.approve(firstApproval.id(), director.getEmail());
        CashRequestResponse amountSet = cashRequestService.setFinalAmount(
                secondApproval.id(), new BigDecimal("240000"), "Estimation ajustée", accountant.getEmail());

        CashVoucherResponse voucher = cashVoucherService.issue(
                amountSet.id(), "Avance pour mission terrain", accountant.getEmail());

        assertEquals("000001", voucher.voucherNumber());
        assertEquals(CashVoucherType.SORTIE, voucher.type());
        assertEquals(CashVoucherStatus.EMIS, voucher.status());
        assertEquals(CashRequestStatus.BON_EMIS, voucher.requestStatus());
        assertEquals(new BigDecimal("240000"), voucher.amount());
        assertEquals("deux cent quarante mille francs CFA", voucher.amountInWords());
        assertEquals(1, cashVoucherRepository.count());
        assertEquals(6, eventRepository.count());
        assertThrows(IllegalArgumentException.class,
                () -> cashVoucherService.issue(amountSet.id(), "Deuxième émission", accountant.getEmail()));

        CashRequestResponse nextRequest = cashRequestService.create(
                payload("Deuxième mission", new BigDecimal("100000")), requester.getEmail());
        CashRequestResponse nextSubmitted = cashRequestService.submit(nextRequest.id(), requester.getEmail());
        CashRequestResponse nextN1 = cashRequestService.approve(nextSubmitted.id(), manager.getEmail());
        CashRequestResponse nextApproved = cashRequestService.approve(nextN1.id(), director.getEmail());
        CashRequestResponse nextAmount = cashRequestService.setFinalAmount(
                nextApproved.id(), new BigDecimal("100000"), null, accountant.getEmail());
        CashVoucherResponse nextVoucher = cashVoucherService.issue(
                nextAmount.id(), "Seconde avance", accountant.getEmail());

        assertEquals("000002", nextVoucher.voucherNumber());
        assertEquals(2, cashVoucherRepository.count());
    }

    @Test
    void doesNotIssueVoucherBeforeAccountingHasFixedTheAmount() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount requester = createEmployee("EMP-001", "employee1", director, null);
        UserAccount accountant = createAccountant("ACC-001", "accountant1", director);
        CashRequestResponse draft = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());

        assertThrows(IllegalArgumentException.class,
                () -> cashVoucherService.issue(draft.id(), "Avance mission", accountant.getEmail()));
        assertEquals(0, cashVoucherRepository.count());
    }

    @Test
    void requiresThreeDifferentSignedVisasBeforeRecordingDisbursement() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount manager = createEmployee("MGR-001", "manager1", director, null);
        UserAccount requester = createEmployee("EMP-001", "employee1", manager, director);
        UserAccount accountant = createAccountant("ACC-001", "accountant1", director);
        saveTestSignature(director);
        saveTestSignature(requester);
        saveTestSignature(accountant);

        CashRequestResponse created = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());
        CashRequestResponse submitted = cashRequestService.submit(created.id(), requester.getEmail());
        CashRequestResponse n1 = cashRequestService.approve(submitted.id(), manager.getEmail());
        CashRequestResponse approved = cashRequestService.approve(n1.id(), director.getEmail());
        CashRequestResponse amountSet = cashRequestService.setFinalAmount(
                approved.id(), new BigDecimal("250000"), null, accountant.getEmail());
        CashVoucherResponse issued = cashVoucherService.issue(
                amountSet.id(), "Avance mission", accountant.getEmail());

        assertThrows(IllegalArgumentException.class, () -> cashVoucherService.disburse(
                created.id(), CashFundOrigin.NSIA, LocalDate.of(2026, 11, 11), accountant.getEmail()));
        assertThrows(IllegalArgumentException.class, () -> cashVoucherService.sign(
                created.id(), CashVoucherVisaType.BENEFICIAIRE, requester.getEmail(), Instant.now()));

        Instant now = Instant.now();
        CashVoucherResponse withCashVisa = cashVoucherService.sign(
                created.id(), CashVoucherVisaType.CAISSE, accountant.getEmail(), now);
        assertEquals(1, withCashVisa.visas().size());
        assertThrows(IllegalArgumentException.class, () -> cashVoucherService.sign(
                created.id(), CashVoucherVisaType.BENEFICIAIRE, requester.getEmail(), Instant.now()));

        assertThrows(ResponseStatusException.class, () -> cashVoucherService.sign(
                created.id(), CashVoucherVisaType.DIRECTION, accountant.getEmail(), Instant.now()));
        assertEquals(1, cashVoucherVisaRepository.count());

        CashVoucherResponse withDirectionVisa = cashVoucherService.sign(
                created.id(), CashVoucherVisaType.DIRECTION, director.getEmail(), Instant.now());
        CashVoucherResponse withAllVisas = cashVoucherService.sign(
                created.id(), CashVoucherVisaType.BENEFICIAIRE, requester.getEmail(), Instant.now());
        assertEquals(3, withAllVisas.visas().size());

        CashVoucherResponse disbursed = cashVoucherService.disburse(
                created.id(), CashFundOrigin.NSIA, LocalDate.of(2026, 11, 11), accountant.getEmail());

        assertEquals(CashVoucherStatus.DECAISSE, disbursed.status());
        assertEquals(CashRequestStatus.DECAISSEE, disbursed.requestStatus());
        assertEquals(CashFundOrigin.NSIA, disbursed.fundOrigin());
        assertEquals(3, disbursed.visas().size());
        assertEquals(10, eventRepository.count());
    }

    private void saveTestSignature(UserAccount account) {
        userSignatureRepository.save(new UserSignature(
                account.getId(),
                new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a },
                Instant.now()));
    }

    @Test
    void preventsTheRequesterFromIssuingTheirOwnVoucher() {
        UserAccount director = createDirector("DG-001", "dg1");
        UserAccount manager = createEmployee("MGR-001", "manager1", director, null);
        UserAccount requester = createEmployee("EMP-001", "employee1", manager, director);
        UserAccount accountant = createAccountant("ACC-001", "accountant1", director);

        CashRequestResponse created = cashRequestService.create(
                payload("Mission terrain", new BigDecimal("250000")), requester.getEmail());
        CashRequestResponse submitted = cashRequestService.submit(created.id(), requester.getEmail());
        CashRequestResponse firstApproval = cashRequestService.approve(submitted.id(), manager.getEmail());
        CashRequestResponse approved = cashRequestService.approve(firstApproval.id(), director.getEmail());
        cashRequestService.setFinalAmount(
                approved.id(), new BigDecimal("250000"), null, accountant.getEmail());

        assertThrows(ResponseStatusException.class,
                () -> cashVoucherService.issue(created.id(), "Avance mission", requester.getEmail()));
        assertEquals(0, cashVoucherRepository.count());
    }

    private CashRequestPayload payload(String subject, BigDecimal estimatedAmount) {
        return new CashRequestPayload(
                subject,
                "Dakar",
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 25),
                "Transport",
                estimatedAmount,
                "facture.pdf");
    }

    private UserAccount createDirector(String employeeId, String emailPrefix) {
        return accountService.create(account(
                employeeId, emailPrefix, AccountRole.DIRECTEUR_GENERAL, null, null));
    }

    private UserAccount createEmployee(
            String employeeId,
            String emailPrefix,
            UserAccount managerN1,
            UserAccount managerN2) {
        return accountService.create(account(
                employeeId,
                emailPrefix,
                null,
                managerN1 == null ? null : managerN1.getId(),
                managerN2 == null ? null : managerN2.getId()));
    }

    private UserAccount createAccountant(String employeeId, String emailPrefix, UserAccount director) {
        return accountService.create(account(
                employeeId, emailPrefix, AccountRole.COMPTABLE, director.getId(), null));
    }

    private AccountRequest account(
            String employeeId,
            String emailPrefix,
            AccountRole role,
            java.util.UUID managerN1Id,
            java.util.UUID managerN2Id) {
        return new AccountRequest(
                employeeId,
                emailPrefix,
                "Test",
                emailPrefix + "@digin.example",
                "Direction",
                "Collaborateur",
                role,
                managerN1Id,
                managerN2Id);
    }
}
