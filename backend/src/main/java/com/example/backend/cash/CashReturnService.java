package com.example.backend.cash;

import com.example.backend.accounts.AccountNotFoundException;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import com.example.backend.security.UserSignature;
import com.example.backend.security.UserSignatureRepository;
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
public class CashReturnService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final CashRequestRepository requests;
    private final CashRequestService requestService;
    private final CashVoucherRepository vouchers;
    private final CashVoucherSequenceRepository sequences;
    private final CashEntryVoucherRepository entryVouchers;
    private final CashReturnAuthorizationRepository authorizations;
    private final SupportingDocumentRepository documents;
    private final CashRequestEventRepository events;
    private final UserAccountRepository accounts;
    private final CashEntryVoucherVisaRepository entryVisas;
    private final UserSignatureRepository signatures;
    private final Clock clock;
    private final int visaReauthenticationMinutes;

    @Autowired
    public CashReturnService(
            CashRequestRepository requests,
            CashRequestService requestService,
            CashVoucherRepository vouchers,
            CashVoucherSequenceRepository sequences,
            CashEntryVoucherRepository entryVouchers,
            CashReturnAuthorizationRepository authorizations,
            SupportingDocumentRepository documents,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            CashEntryVoucherVisaRepository entryVisas,
            UserSignatureRepository signatures,
            @Value("${APP_CASH_VISA_REAUTH_MINUTES:5}") int visaReauthenticationMinutes) {
        this(
                requests,
                requestService,
                vouchers,
                sequences,
                entryVouchers,
                authorizations,
                documents,
                events,
                accounts,
                entryVisas,
                signatures,
                Clock.systemUTC(),
                visaReauthenticationMinutes);
    }

    CashReturnService(
            CashRequestRepository requests,
            CashRequestService requestService,
            CashVoucherRepository vouchers,
            CashVoucherSequenceRepository sequences,
            CashEntryVoucherRepository entryVouchers,
            CashReturnAuthorizationRepository authorizations,
            SupportingDocumentRepository documents,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            CashEntryVoucherVisaRepository entryVisas,
            UserSignatureRepository signatures,
            Clock clock,
            int visaReauthenticationMinutes) {
        this.requests = requests;
        this.requestService = requestService;
        this.vouchers = vouchers;
        this.sequences = sequences;
        this.entryVouchers = entryVouchers;
        this.authorizations = authorizations;
        this.documents = documents;
        this.events = events;
        this.accounts = accounts;
        this.entryVisas = entryVisas;
        this.signatures = signatures;
        this.clock = clock;
        this.visaReauthenticationMinutes = visaReauthenticationMinutes;
    }

    @Transactional(readOnly = true)
    public CashRegularizationResponse summary(UUID requestId, String viewerEmail) {
        requestService.findById(requestId, viewerEmail);
        CashRequest request = findRequest(requestId);
        CashVoucher sourceVoucher = findSourceVoucher(requestId);
        return buildSummary(request, sourceVoucher);
    }

    @Transactional
    public CashReturnAuthorizationResponse authorizePartialReturn(
            UUID requestId,
            BigDecimal amount,
            String reason,
            String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        if (actor.getRole() != AccountRole.DIRECTEUR_GENERAL) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Seul le Directeur général peut autoriser un retour ultérieur.");
        }
        requestService.findById(requestId, actorEmail);
        CashRequest request = lockRequest(requestId);
        requireRegularization(request);
        if (request.getRequester() != null && request.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Le bénéficiaire ne peut pas autoriser son propre retour de fonds.");
        }
        CashVoucher sourceVoucher = findLockedSourceVoucher(requestId);
        validateReason(reason);
        validatePositiveIntegerAmount(amount);

        BigDecimal balance = outstandingBalance(requestId, sourceVoucher);
        if (entryVouchers.countByRequestId(requestId) == 0) {
            throw new IllegalArgumentException(
                    "L’autorisation du Directeur général ne concerne que les retours après le premier.");
        }
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    "Le montant autorisé ne peut pas dépasser le solde restant à régulariser.");
        }

        Instant now = clock.instant();
        CashReturnAuthorization authorization = authorizations.save(new CashReturnAuthorization(
                UUID.randomUUID(), request, amount, reason, actor, now));
        recordEvent(request, actor, "CASH_RETURN_AUTHORIZED", reason.trim(), now);
        return CashReturnAuthorizationResponse.from(authorization);
    }

    @Transactional
    public CashEntryVoucherResponse issueEntryVoucher(
            UUID requestId,
            CashEntryVoucherPayload payload,
            String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requestService.findById(requestId, actorEmail);
        CashRequest request = lockRequest(requestId);
        requireRegularization(request);
        requireAccountingPermission(actor, request);
        CashVoucher sourceVoucher = findLockedSourceVoucher(requestId);
        validatePositiveIntegerAmount(payload.amount());
        validateReason(payload.reason());
        LocalDate today = LocalDate.now(clock);
        if (payload.returnedOn().isAfter(today)
                || payload.returnedOn().isBefore(sourceVoucher.getDisbursedOn())) {
            throw new IllegalArgumentException(
                    "La date du retour doit être comprise entre le décaissement et aujourd’hui.");
        }

        BigDecimal balanceBefore = outstandingBalance(requestId, sourceVoucher);
        if (payload.amount().compareTo(balanceBefore) > 0) {
            throw new IllegalArgumentException(
                    "Le montant du bon d’entrée dépasse le solde à régulariser de "
                            + balanceBefore.toPlainString() + " FCFA.");
        }
        BigDecimal balanceAfter = balanceBefore.subtract(payload.amount());
        boolean repeatedReturn = entryVouchers.countByRequestId(requestId) > 0;
        CashReturnAuthorization authorization = null;
        if (repeatedReturn) {
            if (payload.authorizationId() == null) {
                throw new IllegalArgumentException(
                        "Tout retour après le premier nécessite une autorisation préalable du Directeur général.");
            }
            authorization = authorizations.lockByIdAndRequestId(payload.authorizationId(), requestId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "L’autorisation du Directeur général est introuvable pour cette demande."));
            if (authorization.getConsumedAt() != null
                    || authorization.getAuthorizedAmount().compareTo(payload.amount()) != 0) {
                throw new IllegalArgumentException(
                        "L’autorisation doit être disponible et porter exactement sur le montant retourné.");
            }
            authorization.consume(clock.instant());
        } else if (payload.authorizationId() != null) {
            throw new IllegalArgumentException(
                    "Aucune autorisation n’est requise pour le premier retour.");
        }

        CashVoucherSequence sequence = sequences.lockById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "La séquence des bons de caisse n’est pas initialisée."));
        String number = "E-" + String.format(java.util.Locale.ROOT, "%06d", sequence.reserveNextNumber());
        Instant now = clock.instant();
        CashEntryVoucher entryVoucher = entryVouchers.save(new CashEntryVoucher(
                UUID.randomUUID(),
                number,
                request,
                sourceVoucher,
                actor,
                authorization,
                payload.amount(),
                payload.reason(),
                payload.returnedOn(),
                payload.fundOrigin() == null ? sourceVoucher.getFundOrigin() : payload.fundOrigin(),
                now,
                balanceBefore,
                balanceAfter));
        recordEvent(request, actor, "CASH_ENTRY_VOUCHER_ISSUED", payload.reason().trim(), now);
        return CashEntryVoucherResponse.from(entryVoucher);
    }

    @Transactional
    public CashEntryVoucherResponse signEntryVoucher(
            UUID requestId,
            UUID entryVoucherId,
            CashVoucherVisaType type,
            String actorEmail,
            Instant reauthenticatedAt) {
        UserAccount actor = findActiveAccount(actorEmail);
        requestService.findById(requestId, actorEmail);
        CashRequest request = lockRequest(requestId);
        requireRegularization(request);
        CashEntryVoucher voucher = entryVouchers.lockByIdAndRequestId(entryVoucherId, requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Le bon d’entrée est introuvable pour cette demande."));
        if (entryVisas.existsByEntryVoucherIdAndType(voucher.getId(), type)) {
            throw new IllegalArgumentException("Ce visa a déjà été apposé.");
        }
        requireEntryVisaPermission(actor, request, type);
        if (type == CashVoucherVisaType.BENEFICIAIRE
                && (!entryVisas.existsByEntryVoucherIdAndType(voucher.getId(), CashVoucherVisaType.DIRECTION)
                        || !entryVisas.existsByEntryVoucherIdAndType(voucher.getId(), CashVoucherVisaType.CAISSE))) {
            throw new IllegalArgumentException(
                    "Le bénéficiaire appose son visa en dernier, après les visas Direction et Caisse.");
        }
        if (entryVisas.existsByEntryVoucherIdAndSignerId(voucher.getId(), actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Une même personne ne peut pas apposer plusieurs visas sur ce bon.");
        }

        Instant now = clock.instant();
        requireRecentReauthentication(reauthenticatedAt, now);
        UserSignature signature = signatures.findById(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Enregistrez votre signature avant de viser le bon."));
        CashEntryVoucherVisa visa = new CashEntryVoucherVisa(
                UUID.randomUUID(),
                voucher,
                type,
                actor,
                now,
                reauthenticatedAt,
                signature.getSignaturePng());
        entryVisas.save(visa);
        voucher.addVisa(visa);
        recordEvent(request, actor, "VISA_ENTRY_" + type.name(), "", now);
        return CashEntryVoucherResponse.from(voucher);
    }

    @Transactional
    public void assertDocumentAcceptance(UUID requestId, BigDecimal amount) {
        CashRequest request = lockRequest(requestId);
        if (request.getStatus() != CashRequestStatus.DECAISSEE
                && request.getStatus() != CashRequestStatus.EN_REGULARISATION) {
            throw new IllegalArgumentException("La demande ne peut pas être régularisée à cette étape.");
        }
        CashVoucher sourceVoucher = findLockedSourceVoucher(requestId);
        BigDecimal balance = outstandingBalance(requestId, sourceVoucher);
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    "Le montant accepté dépasserait le solde restant à régulariser.");
        }
    }

    @Transactional
    public void closeRegularization(UUID requestId, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requestService.findById(requestId, actorEmail);
        CashRequest request = lockRequest(requestId);
        if (request.getStatus() != CashRequestStatus.EN_REGULARISATION) {
            throw new IllegalArgumentException("La régularisation ne peut être clôturée qu’en phase de régularisation.");
        }
        CashVoucher sourceVoucher = findLockedSourceVoucher(requestId);
        BigDecimal balance = outstandingBalance(requestId, sourceVoucher);
        if (balance.signum() != 0) {
            throw new IllegalArgumentException(
                    "Le solde de régularisation n’est pas nul (" + balance.toPlainString() + " FCFA).");
        }
        if (documents.existsByRequestIdAndStatus(requestId, SupportingDocumentStatus.A_CONTROLER)) {
            throw new IllegalArgumentException(
                    "Des justificatifs attendent encore le contrôle comptable.");
        }
        boolean entryVoucherWithoutVisas = entryVouchers.findAllByRequestIdOrderByIssuedAtDesc(requestId).stream()
                .anyMatch(voucher -> voucher.getVisas().size() < 3);
        if (entryVoucherWithoutVisas) {
            throw new IllegalArgumentException(
                    "Les trois visas doivent être apposés sur chaque bon d’entrée avant la clôture.");
        }
        requireAccountingPermission(actor, request);
        Instant now = clock.instant();
        request.closeRegularization(now);
        requests.save(request);
        recordEvent(request, actor, "REGULARIZATION_CLOSED", "Régularisation clôturée : solde à zéro", now);
    }

    private CashRegularizationResponse buildSummary(CashRequest request, CashVoucher sourceVoucher) {
        UUID requestId = request.getId();
        BigDecimal accepted = acceptedAmount(requestId);
        BigDecimal returned = returnedAmount(requestId);
        BigDecimal balance = sourceVoucher.getAmount().subtract(accepted).subtract(returned);
        if (balance.signum() < 0) {
            throw new IllegalStateException("Le solde de régularisation est négatif; vérifiez les pièces comptables.");
        }
        List<CashEntryVoucherResponse> entries = entryVouchers
                .findAllByRequestIdOrderByIssuedAtDesc(requestId).stream()
                .map(CashEntryVoucherResponse::from)
                .toList();
        List<CashReturnAuthorizationResponse> approvals = authorizations
                .findAllByRequestIdOrderByAuthorizedAtDesc(requestId).stream()
                .map(CashReturnAuthorizationResponse::from)
                .toList();
        return new CashRegularizationResponse(
                requestId,
                sourceVoucher.getVoucherNumber(),
                sourceVoucher.getAmount(),
                accepted,
                returned,
                balance,
                countPartialReturns(requestId),
                entries,
                approvals);
    }

    private BigDecimal outstandingBalance(UUID requestId, CashVoucher sourceVoucher) {
        BigDecimal balance = sourceVoucher.getAmount()
                .subtract(acceptedAmount(requestId))
                .subtract(returnedAmount(requestId));
        if (balance.signum() < 0) {
            throw new IllegalStateException("Le solde de régularisation est négatif; vérifiez les pièces comptables.");
        }
        return balance;
    }

    private BigDecimal acceptedAmount(UUID requestId) {
        return documents.sumAmountByRequestIdAndStatus(requestId, SupportingDocumentStatus.ACCEPTE);
    }

    private BigDecimal returnedAmount(UUID requestId) {
        return entryVouchers.sumAmountByRequestId(requestId);
    }

    private long countPartialReturns(UUID requestId) {
        return entryVouchers.countByRequestIdAndBalanceAfterGreaterThan(requestId, ZERO);
    }

    private CashRequest lockRequest(UUID requestId) {
        return requests.lockById(requestId)
                .orElseThrow(() -> new CashRequestNotFoundException("La demande de caisse est introuvable."));
    }

    private CashRequest findRequest(UUID requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new CashRequestNotFoundException("La demande de caisse est introuvable."));
    }

    private CashVoucher findSourceVoucher(UUID requestId) {
        return vouchers.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Le bon de sortie d’origine est introuvable."));
    }

    private CashVoucher findLockedSourceVoucher(UUID requestId) {
        CashVoucher voucher = vouchers.lockByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Le bon de sortie d’origine est introuvable."));
        if (voucher.getStatus() != CashVoucherStatus.DECAISSE || voucher.getDisbursedOn() == null) {
            throw new IllegalArgumentException("Le bon de sortie n’a pas encore été décaissé.");
        }
        return voucher;
    }

    private void requireRegularization(CashRequest request) {
        if (request.getStatus() != CashRequestStatus.EN_REGULARISATION) {
            throw new IllegalArgumentException("Le bon d’entrée n’est disponible qu’en phase de régularisation.");
        }
    }

    private void requireAccountingPermission(UserAccount actor, CashRequest request) {
        if (request.getRequester() == null || request.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Le bénéficiaire ne peut pas enregistrer son propre retour de fonds.");
        }
        boolean allowed = actor.getRole() == AccountRole.COMPTABLE
                || (actor.getRole() == AccountRole.ADJOINT_COMPTABLE
                        && request.getRequester().getRole() == AccountRole.COMPTABLE);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L’émission du bon d’entrée est réservée à la comptabilité.");
        }
    }

    private void requireEntryVisaPermission(UserAccount actor, CashRequest request, CashVoucherVisaType type) {
        if (request.getRequester() == null) {
            throw new IllegalArgumentException("Le bénéficiaire du bon n’est pas défini.");
        }
        boolean isRequester = request.getRequester().getId().equals(actor.getId());
        boolean allowed = switch (type) {
            case BENEFICIAIRE -> isRequester;
            case DIRECTION -> !isRequester
                    && ((request.getValidatorN2() != null
                            && request.getValidatorN2().getId().equals(actor.getId()))
                            || (request.getValidatorN2() == null
                                    && request.getValidatorN1() != null
                                    && request.getValidatorN1().getRole() == AccountRole.DIRECTEUR_GENERAL
                                    && request.getValidatorN1().getId().equals(actor.getId())));
            case CAISSE -> !isRequester
                    && (actor.getRole() == AccountRole.COMPTABLE
                            || (actor.getRole() == AccountRole.ADJOINT_COMPTABLE
                                    && request.getRequester().getRole() == AccountRole.COMPTABLE));
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n’êtes pas autorisé à apposer ce visa.");
        }
    }

    private void requireRecentReauthentication(Instant reauthenticatedAt, Instant now) {
        if (reauthenticationWindowInvalid()) {
            throw new IllegalStateException("APP_CASH_VISA_REAUTH_MINUTES doit être compris entre 1 et 60.");
        }
        if (reauthenticatedAt == null
                || reauthenticatedAt.isAfter(now)
                || reauthenticatedAt.isBefore(now.minusSeconds(visaReauthenticationMinutes * 60L))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Confirmez à nouveau votre identité avant d’apposer le visa.");
        }
    }

    private boolean reauthenticationWindowInvalid() {
        return visaReauthenticationMinutes < 1 || visaReauthenticationMinutes > 60;
    }

    private void validatePositiveIntegerAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Le montant doit être un nombre entier supérieur à zéro.");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 1000) {
            throw new IllegalArgumentException("Un motif de retour obligatoire (1 000 caractères maximum) est requis.");
        }
    }

    private UserAccount findActiveAccount(String email) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccountNotFoundException("Le compte connecté n’existe plus."));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le compte est désactivé.");
        }
        return account;
    }

    private void recordEvent(CashRequest request, UserAccount actor, String action, String reason, Instant at) {
        events.save(new CashRequestEvent(
                UUID.randomUUID(),
                request.getId(),
                actor.getId(),
                action,
                request.getStatus().name(),
                request.getStatus().name(),
                reason,
                at));
    }
}
