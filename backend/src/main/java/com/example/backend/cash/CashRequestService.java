package com.example.backend.cash;

import com.example.backend.accounts.AccountNotFoundException;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CashRequestService {

    private final CashRequestRepository requests;
    private final CashRequestEventRepository events;
    private final UserAccountRepository accounts;
    private final Clock clock;
    private final int minimumRejectionReasonLength;

    @Autowired
    public CashRequestService(
            CashRequestRepository requests,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            @Value("${app.cash.rejection-min-length:10}") int minimumRejectionReasonLength) {
        this(requests, events, accounts, Clock.systemUTC(), minimumRejectionReasonLength);
    }

    CashRequestService(
            CashRequestRepository requests,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            Clock clock,
            int minimumRejectionReasonLength) {
        this.requests = requests;
        this.events = events;
        this.accounts = accounts;
        this.clock = clock;
        this.minimumRejectionReasonLength = minimumRejectionReasonLength;
    }

    @Transactional
    public int regularizeExpiredRequests() {
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();
        List<CashRequest> expiredRequests = requests.findAllByStatusAndEndDateBefore(
                CashRequestStatus.DECAISSEE, today);
        for (CashRequest request : expiredRequests) {
            CashRequestStatus previousStatus = request.getStatus();
            request.markInRegularization(today, now);
            requests.save(request);
            events.save(new CashRequestEvent(
                    UUID.randomUUID(),
                    request.getId(),
                    null,
                    "SYSTEM_REGULARIZATION_STARTED",
                    previousStatus.name(),
                    request.getStatus().name(),
                    null,
                    now));
        }
        return expiredRequests.size();
    }

    @Transactional(readOnly = true)
    public List<CashRequestResponse> findAll(String viewerEmail) {
        UserAccount viewer = findActiveAccount(viewerEmail);
        List<CashRequest> visibleRequests = canSeeAllRequests(viewer)
                ? requests.findAllByOrderByCreatedAtDesc()
                : requests.findVisibleTo(viewer.getId());
        return visibleRequests.stream().map(CashRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CashRequestResponse findById(UUID id, String viewerEmail) {
        UserAccount viewer = findActiveAccount(viewerEmail);
        CashRequest request = findRequest(id);
        requireVisible(viewer, request);
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse create(CashRequestPayload payload, String requesterEmail) {
        validateRequest(payload);
        UserAccount requester = findActiveAccount(requesterEmail);
        if (requester.getRole() == AccountRole.ADMINISTRATEUR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L’administrateur ne peut pas créer de demande de caisse.");
        }

        Instant now = clock.instant();
        CashRequest request = new CashRequest(
                UUID.randomUUID(),
                payload.subject(),
                payload.location(),
                payload.startDate(),
                payload.endDate(),
                payload.category(),
                payload.estimatedAmount(),
                payload.attachmentName(),
                requester,
                now,
                now);
        requests.save(request);
        recordEvent(request, requester, "CREATED", null, null);
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse update(UUID id, CashRequestPayload payload, String requesterEmail) {
        validateRequest(payload);
        UserAccount requester = findActiveAccount(requesterEmail);
        CashRequest request = findRequest(id);
        requireRequester(requester, request);
        if (request.getStatus() != CashRequestStatus.DRAFT
                && request.getStatus() != CashRequestStatus.SOUMISE
                && request.getStatus() != CashRequestStatus.A_CORRIGER) {
            throw new IllegalArgumentException("La demande ne peut plus être modifiée à cette étape.");
        }

        CashRequestStatus previousStatus = request.getStatus();
        Instant now = clock.instant();
        request.updateDetails(
                payload.subject(),
                payload.location(),
                payload.startDate(),
                payload.endDate(),
                payload.category(),
                payload.estimatedAmount(),
                payload.attachmentName(),
                now);
        requests.save(request);
        recordEvent(request, requester, "MODIFIED", previousStatus, null);
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse submit(UUID id, String requesterEmail) {
        UserAccount requester = findActiveAccount(requesterEmail);
        CashRequest request = findRequest(id);
        requireRequester(requester, request);
        if (requester.getRole() == AccountRole.DIRECTEUR_GENERAL) {
            throw new IllegalArgumentException(
                    "Le valideur désigné pour les demandes de la Direction doit être configuré avant la soumission.");
        }
        UserAccount managerN1 = requester.getManagerN1();
        UserAccount managerN2 = requester.getManagerN2();
        if (managerN1 != null && managerN1.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Le responsable N+1 est désactivé et doit être remplacé.");
        }
        if (managerN2 != null && managerN2.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Le responsable N+2 est désactivé et doit être remplacé.");
        }

        CashRequestStatus previousStatus = request.getStatus();
        Instant now = clock.instant();
        request.submit(managerN1, managerN2, now);
        requests.save(request);
        recordEvent(request, requester, "SUBMITTED", previousStatus, null);
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse approve(UUID id, String validatorEmail) {
        UserAccount validator = findActiveAccount(validatorEmail);
        CashRequest request = findRequest(id);
        CashRequestStatus previousStatus = request.getStatus();
        Instant now = clock.instant();

        if (previousStatus == CashRequestStatus.SOUMISE) {
            requireValidator(validator, request.getValidatorN1(), "N+1");
            request.approveN1(now);
        } else if (previousStatus == CashRequestStatus.VALIDEE_N1) {
            requireValidator(validator, request.getValidatorN2(), "N+2");
            request.approveN2(now);
        } else {
            throw new IllegalArgumentException("Cette demande n’attend pas une validation hiérarchique.");
        }

        requests.save(request);
        recordEvent(
                request,
                validator,
                previousStatus == CashRequestStatus.SOUMISE ? "N1_APPROVED" : "N2_APPROVED",
                previousStatus,
                null);
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse reject(UUID id, String reason, String validatorEmail) {
        UserAccount validator = findActiveAccount(validatorEmail);
        if (reason == null || reason.trim().length() < minimumRejectionReasonLength) {
            throw new IllegalArgumentException(
                    "Le motif du rejet doit contenir au moins " + minimumRejectionReasonLength + " caractères.");
        }
        CashRequest request = findRequest(id);
        CashRequestStatus previousStatus = request.getStatus();
        if (previousStatus == CashRequestStatus.SOUMISE) {
            requireValidator(validator, request.getValidatorN1(), "N+1");
        } else if (previousStatus == CashRequestStatus.VALIDEE_N1) {
            requireValidator(validator, request.getValidatorN2(), "N+2");
        } else {
            throw new IllegalArgumentException("Cette demande n’attend pas une décision hiérarchique.");
        }

        Instant now = clock.instant();
        request.reject(reason, now);
        requests.save(request);
        recordEvent(request, validator, "REJECTED", previousStatus, reason.trim());
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse cancel(UUID id, String reason, String requesterEmail) {
        UserAccount requester = findActiveAccount(requesterEmail);
        CashRequest request = findRequest(id);
        requireRequester(requester, request);
        CashRequestStatus previousStatus = request.getStatus();
        Instant now = clock.instant();
        request.cancel(reason, now);
        requests.save(request);
        recordEvent(request, requester, "CANCELLED", previousStatus, reason.trim());
        return CashRequestResponse.from(request);
    }

    @Transactional
    public CashRequestResponse setFinalAmount(
            UUID id,
            BigDecimal finalAmount,
            String reason,
            String accountantEmail) {
        UserAccount accountant = findActiveAccount(accountantEmail);
        CashRequest request = findRequest(id);
        requireAccountingPermission(accountant, request);
        if (reason != null && reason.length() > 1000) {
            throw new IllegalArgumentException("Le motif ne peut pas dépasser 1 000 caractères.");
        }

        CashRequestStatus previousStatus = request.getStatus();
        Instant now = clock.instant();
        request.setFinalAmount(finalAmount, reason, now);
        requests.save(request);
        recordEvent(
                request,
                accountant,
                "FINAL_AMOUNT_SET",
                previousStatus,
                reason == null || reason.isBlank() ? null : reason.trim());
        return CashRequestResponse.from(request);
    }

    private void requireAccountingPermission(UserAccount actor, CashRequest request) {
        if (request.getStatus() != CashRequestStatus.VALIDEE) {
            throw new IllegalArgumentException("Seule une demande validée peut être traitée par la comptabilité.");
        }
        if (request.getRequester() == null || request.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas traiter votre propre demande.");
        }
        AccountRole requesterRole = request.getRequester().getRole();
        boolean permitted = actor.getRole() == AccountRole.COMPTABLE
                || (actor.getRole() == AccountRole.ADJOINT_COMPTABLE && requesterRole == AccountRole.COMPTABLE);
        if (!permitted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette opération est réservée à la comptabilité.");
        }
    }

    private void requireValidator(UserAccount actor, UserAccount expected, String level) {
        if (expected == null || !expected.getId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seul le responsable " + level + " enregistré peut prendre cette décision.");
        }
    }

    private void requireRequester(UserAccount actor, CashRequest request) {
        if (request.getRequester() == null || !request.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le demandeur peut modifier cette demande.");
        }
    }

    private void requireVisible(UserAccount viewer, CashRequest request) {
        if (!canSeeAllRequests(viewer)
                && (request.getRequester() == null
                        || (!request.getRequester().getId().equals(viewer.getId())
                                && (request.getValidatorN1() == null
                                        || !request.getValidatorN1().getId().equals(viewer.getId()))
                                && (request.getValidatorN2() == null
                                        || !request.getValidatorN2().getId().equals(viewer.getId()))))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette demande n’existe pas.");
        }
    }

    private boolean canSeeAllRequests(UserAccount account) {
        return account.getRole() == AccountRole.DIRECTEUR_GENERAL
                || account.getRole() == AccountRole.COMPTABLE
                || account.getRole() == AccountRole.ADJOINT_COMPTABLE;
    }

    private UserAccount findActiveAccount(String email) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccountNotFoundException("Le compte connecté n’existe plus."));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le compte est désactivé.");
        }
        return account;
    }

    private CashRequest findRequest(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new CashRequestNotFoundException(
                        "Aucune demande de caisse ne correspond à l’identifiant " + id + "."));
    }

    private void recordEvent(
            CashRequest request,
            UserAccount actor,
            String action,
            CashRequestStatus previousStatus,
            String reason) {
        events.save(new CashRequestEvent(
                UUID.randomUUID(),
                request.getId(),
                actor.getId(),
                action,
                previousStatus == null ? null : previousStatus.name(),
                request.getStatus().name(),
                reason,
                clock.instant()));
    }

    private void validateRequest(CashRequestPayload request) {
        if (request == null) {
            throw new IllegalArgumentException("Les informations de la demande sont obligatoires.");
        }
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("La date de fin doit être supérieure ou égale à la date de début.");
        }
        if (request.estimatedAmount() != null
                && (request.estimatedAmount().compareTo(BigDecimal.ZERO) <= 0
                        || request.estimatedAmount().stripTrailingZeros().scale() > 0)) {
            throw new IllegalArgumentException("Le montant estimé doit être un entier supérieur à zéro.");
        }
    }
}
