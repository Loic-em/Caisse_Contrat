package com.example.backend.cash;

import com.example.backend.accounts.AccountNotFoundException;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import com.example.backend.security.UserSignature;
import com.example.backend.security.UserSignatureRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CashVoucherService {

    private final CashRequestRepository requests;
    private final CashVoucherRepository vouchers;
    private final CashVoucherSequenceRepository sequences;
    private final CashRequestEventRepository events;
    private final UserAccountRepository accounts;
    private final CashVoucherVisaRepository visas;
    private final UserSignatureRepository signatures;
    private final Clock clock;
    private final String firstVoucherNumber;
    private final int visaReauthenticationMinutes;

    @Autowired
    public CashVoucherService(
            CashRequestRepository requests,
            CashVoucherRepository vouchers,
            CashVoucherSequenceRepository sequences,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            CashVoucherVisaRepository visas,
            UserSignatureRepository signatures,
            @Value("${APP_CASH_VOUCHER_FIRST_NUMBER:1}") String firstVoucherNumber,
            @Value("${APP_CASH_VISA_REAUTH_MINUTES:5}") int visaReauthenticationMinutes) {
        this(
                requests,
                vouchers,
                sequences,
                events,
                accounts,
                visas,
                signatures,
                Clock.systemUTC(),
                firstVoucherNumber,
                visaReauthenticationMinutes);
    }

    CashVoucherService(
            CashRequestRepository requests,
            CashVoucherRepository vouchers,
            CashVoucherSequenceRepository sequences,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            CashVoucherVisaRepository visas,
            UserSignatureRepository signatures,
            Clock clock,
            String firstVoucherNumber,
            int visaReauthenticationMinutes) {
        this.requests = requests;
        this.vouchers = vouchers;
        this.sequences = sequences;
        this.events = events;
        this.accounts = accounts;
        this.visas = visas;
        this.signatures = signatures;
        this.clock = clock;
        this.firstVoucherNumber = firstVoucherNumber;
        this.visaReauthenticationMinutes = visaReauthenticationMinutes;
    }

    @Transactional
    public CashVoucherResponse issue(UUID requestId, String reason, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        CashRequest request = findRequest(requestId);
        requireAccountingPermission(actor, request);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Le motif du bon est obligatoire.");
        }
        if (reason.trim().length() > 1000) {
            throw new IllegalArgumentException("Le motif du bon ne peut pas dépasser 1 000 caractères.");
        }
        if (request.getStatus() != CashRequestStatus.MONTANT_ARRETE) {
            throw new IllegalArgumentException("Le bon ne peut être émis qu’après l’arrêt du montant définitif.");
        }
        if (vouchers.findByRequestId(requestId).isPresent()) {
            throw new IllegalArgumentException("Un bon a déjà été émis pour cette demande.");
        }

        CashVoucherSequence sequence = sequences.lockById(1L)
                .orElseThrow(() -> new IllegalStateException("La séquence des bons de caisse n’est pas initialisée."));
        if (vouchers.count() == 0) {
            sequence.setInitialNumber(parseFirstVoucherNumber());
        }
        long number = sequence.reserveNextNumber();
        String voucherNumber = String.format(java.util.Locale.ROOT, "%06d", number);
        Instant now = clock.instant();

        CashVoucher voucher = new CashVoucher(
                UUID.randomUUID(),
                voucherNumber,
                request,
                actor,
                reason,
                now,
                FrenchAmountInWords.format(request.getFinalAmount()));
        CashRequestStatus previousStatus = request.getStatus();
        request.markVoucherIssued(now);
        vouchers.save(voucher);
        events.save(new CashRequestEvent(
                UUID.randomUUID(),
                request.getId(),
                actor.getId(),
                "VOUCHER_ISSUED",
                previousStatus.name(),
                request.getStatus().name(),
                reason.trim(),
                now));
        return CashVoucherResponse.from(voucher);
    }

    private long parseFirstVoucherNumber() {
        try {
            long number = Long.parseLong(firstVoucherNumber);
            if (number <= 0) {
                throw new NumberFormatException("Le numéro doit être supérieur à zéro.");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "APP_CASH_VOUCHER_FIRST_NUMBER doit être un entier positif.", exception);
        }
    }

    @Transactional(readOnly = true)
    public CashVoucherResponse findByRequestId(UUID requestId, String viewerEmail) {
        UserAccount viewer = findActiveAccount(viewerEmail);
        CashRequest request = findRequest(requestId);
        requireVisible(viewer, request);
        CashVoucher voucher = vouchers.findByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun bon n’a été émis pour cette demande."));
        return CashVoucherResponse.from(voucher);
    }

    @Transactional
    public CashVoucherResponse sign(
            UUID requestId,
            CashVoucherVisaType type,
            String actorEmail,
            Instant reauthenticatedAt) {
        UserAccount actor = findActiveAccount(actorEmail);
        CashRequest request = findRequest(requestId);
        if (request.getStatus() != CashRequestStatus.BON_EMIS) {
            throw new IllegalArgumentException("Le bon n’est pas à l’étape des visas.");
        }
        CashVoucher voucher = vouchers.lockByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun bon n’a été émis pour cette demande."));
        if (voucher.getStatus() != CashVoucherStatus.EMIS) {
            throw new IllegalArgumentException("Le bon est verrouillé et ne peut plus recevoir de visa.");
        }
        requireVisaPermission(actor, request, type);
        if (visas.existsByVoucherIdAndType(voucher.getId(), type)) {
            throw new IllegalArgumentException("Ce visa a déjà été apposé.");
        }
        if (type == CashVoucherVisaType.BENEFICIAIRE
                && (!visas.existsByVoucherIdAndType(voucher.getId(), CashVoucherVisaType.DIRECTION)
                        || !visas.existsByVoucherIdAndType(voucher.getId(), CashVoucherVisaType.CAISSE))) {
            throw new IllegalArgumentException(
                    "Le bénéficiaire appose son visa en dernier, après les visas Direction et Caisse.");
        }
        if (visas.existsByVoucherIdAndSignerId(voucher.getId(), actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Une même personne ne peut pas apposer plusieurs visas sur ce bon.");
        }
        Instant now = clock.instant();
        requireRecentReauthentication(actor, reauthenticatedAt, now);
        UserSignature signature = signatures.findById(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Enregistrez votre signature avant de viser le bon."));
        CashVoucherVisa visa = new CashVoucherVisa(
                UUID.randomUUID(),
                voucher,
                type,
                actor,
                now,
                reauthenticatedAt,
                signature.getSignaturePng());
        visas.save(visa);
        voucher.addVisa(visa);
        events.save(new CashRequestEvent(
                UUID.randomUUID(),
                request.getId(),
                actor.getId(),
                "VISA_" + type.name(),
                request.getStatus().name(),
                request.getStatus().name(),
                null,
                now));
        return CashVoucherResponse.from(voucher);
    }

    @Transactional
    public CashVoucherResponse disburse(
            UUID requestId,
            CashFundOrigin fundOrigin,
            java.time.LocalDate disbursedOn,
            String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        CashRequest request = findRequest(requestId);
        requireAccountingPermissionForDisbursement(actor, request);
        CashVoucher voucher = vouchers.lockByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun bon n’a été émis pour cette demande."));
        if (request.getStatus() != CashRequestStatus.BON_EMIS
                || voucher.getStatus() != CashVoucherStatus.EMIS) {
            throw new IllegalArgumentException("Ce bon n’est pas disponible pour un décaissement.");
        }
        for (CashVoucherVisaType required : CashVoucherVisaType.values()) {
            if (!visas.existsByVoucherIdAndType(voucher.getId(), required)) {
                throw new IllegalArgumentException("Les trois visas obligatoires doivent être apposés avant le décaissement.");
            }
        }
        if (fundOrigin == null || disbursedOn == null) {
            throw new IllegalArgumentException("La date et l’origine des fonds sont obligatoires.");
        }
        Instant now = clock.instant();
        CashRequestStatus previousStatus = request.getStatus();
        voucher.markDisbursed(fundOrigin, actor, disbursedOn, now);
        request.markDisbursed(now);
        events.save(new CashRequestEvent(
                UUID.randomUUID(),
                request.getId(),
                actor.getId(),
                "DISBURSED",
                previousStatus.name(),
                request.getStatus().name(),
                fundOrigin.name(),
                now));
        return CashVoucherResponse.from(voucher);
    }

    private void requireVisaPermission(UserAccount actor, CashRequest request, CashVoucherVisaType type) {
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

    private void requireRecentReauthentication(UserAccount actor, Instant reauthenticatedAt, Instant now) {
        if (reauthenticatedAt == null
                || reauthenticatedAt.isAfter(now)
                || reauthenticatedAt.isBefore(now.minusSeconds(visaReauthenticationMinutes * 60L))) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Confirmez à nouveau votre identité avant d’apposer le visa.");
        }
        if (visaReauthenticationMinutes < 1 || visaReauthenticationMinutes > 60) {
            throw new IllegalStateException("APP_CASH_VISA_REAUTH_MINUTES doit être compris entre 1 et 60.");
        }
    }

    private void requireAccountingPermissionForDisbursement(UserAccount actor, CashRequest request) {
        if (request.getRequester() == null || request.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le bénéficiaire ne peut pas enregistrer son propre décaissement.");
        }
        AccountRole requesterRole = request.getRequester().getRole();
        boolean permitted = actor.getRole() == AccountRole.COMPTABLE
                || (actor.getRole() == AccountRole.ADJOINT_COMPTABLE && requesterRole == AccountRole.COMPTABLE);
        if (!permitted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le décaissement est réservé à la comptabilité.");
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

    private CashRequest findRequest(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new CashRequestNotFoundException(
                        "Aucune demande de caisse ne correspond à l’identifiant " + id + "."));
    }

    private void requireAccountingPermission(UserAccount actor, CashRequest request) {
        if (request.getStatus() != CashRequestStatus.MONTANT_ARRETE) {
            throw new IllegalArgumentException("Seule une demande au montant arrêté peut recevoir un bon.");
        }
        if (request.getRequester() == null || request.getRequester().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas émettre le bon de votre propre demande.");
        }
        AccountRole requesterRole = request.getRequester().getRole();
        boolean permitted = actor.getRole() == AccountRole.COMPTABLE
                || (actor.getRole() == AccountRole.ADJOINT_COMPTABLE && requesterRole == AccountRole.COMPTABLE);
        if (!permitted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L’émission du bon est réservée à la comptabilité.");
        }
    }

    private void requireVisible(UserAccount viewer, CashRequest request) {
        boolean canSeeAll = viewer.getRole() == AccountRole.DIRECTEUR_GENERAL
                || viewer.getRole() == AccountRole.COMPTABLE
                || viewer.getRole() == AccountRole.ADJOINT_COMPTABLE;
        boolean isParticipant = request.getRequester() != null
                && request.getRequester().getId().equals(viewer.getId());
        boolean isValidator = (request.getValidatorN1() != null
                && request.getValidatorN1().getId().equals(viewer.getId()))
                || (request.getValidatorN2() != null
                        && request.getValidatorN2().getId().equals(viewer.getId()));
        if (!canSeeAll && !isParticipant && !isValidator) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette demande n’existe pas.");
        }
    }
}
