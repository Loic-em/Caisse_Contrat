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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportingDocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final SupportingDocumentRepository documents;
    private final CashRequestRepository requests;
    private final CashRequestService requestService;
    private final CashRequestEventRepository events;
    private final UserAccountRepository accounts;
    private final AntivirusScanner antivirusScanner;
    private final SupportingDocumentCrypto crypto;
    private final Clock clock;
    private final CashReturnService cashReturnService;

    public SupportingDocumentService(
            SupportingDocumentRepository documents,
            CashRequestRepository requests,
            CashRequestService requestService,
            CashRequestEventRepository events,
            UserAccountRepository accounts,
            AntivirusScanner antivirusScanner,
            SupportingDocumentCrypto crypto,
            CashReturnService cashReturnService) {
        this.documents = documents;
        this.requests = requests;
        this.requestService = requestService;
        this.events = events;
        this.accounts = accounts;
        this.antivirusScanner = antivirusScanner;
        this.crypto = crypto;
        this.cashReturnService = cashReturnService;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<SupportingDocumentResponse> list(UUID requestId, String viewerEmail) {
        requestService.findById(requestId, viewerEmail);
        return documents.findAllByRequestIdOrderByUploadedAtDesc(requestId).stream()
                .map(SupportingDocumentResponse::from)
                .toList();
    }

    @Transactional
    public SupportingDocumentResponse upload(
            UUID requestId,
            String documentType,
            BigDecimal amount,
            LocalDate documentDate,
            UUID replacesId,
            MultipartFile file,
            String actorEmail) {
        CashRequestResponse requestView = requestService.findById(requestId, actorEmail);
        UserAccount actor = findActiveAccount(actorEmail);
        if (!actor.getId().equals(requestView.requesterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seul le bénéficiaire peut déposer ses justificatifs.");
        }
        if (requestView.status() != CashRequestStatus.DECAISSEE) {
            throw new IllegalArgumentException("Les justificatifs peuvent être déposés après le décaissement.");
        }
        if (documentType == null || documentType.isBlank() || documentType.trim().length() > 120
                || amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0
                || documentDate == null || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le type, le fichier, le montant entier et la date de la pièce sont obligatoires.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Chaque justificatif ne peut pas dépasser 10 Mo.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Impossible de lire le fichier transmis.", exception);
        }
        String mediaType = detectMediaType(content);
        antivirusScanner.scan(content);
        byte[] encryptedContent = crypto.encrypt(content);
        SupportingDocument replacedDocument = null;
        if (replacesId != null) {
            replacedDocument = documents.findByIdAndRequestId(replacesId, requestId)
                    .orElseThrow(() -> new CashRequestNotFoundException("Le justificatif remplacé est introuvable."));
            if (replacedDocument.getStatus() != SupportingDocumentStatus.REJETE) {
                throw new IllegalArgumentException("Seul un justificatif rejeté peut être remplacé.");
            }
        }
        CashRequest request = requests.findById(requestId)
                .orElseThrow(() -> new CashRequestNotFoundException("La demande est introuvable."));
        Instant now = clock.instant();
        SupportingDocument document = documents.save(new SupportingDocument(
                UUID.randomUUID(),
                request,
                actor,
                replacedDocument,
                documentType.trim(),
                safeFileName(file.getOriginalFilename()),
                mediaType,
                encryptedContent,
                amount,
                documentDate,
                now));
        recordEvent(request, actor, "SUPPORTING_DOCUMENT_UPLOADED", now);
        return SupportingDocumentResponse.from(document);
    }

    @Transactional
    public SupportingDocumentResponse review(
            UUID requestId,
            UUID documentId,
            SupportingDocumentStatus decision,
            String reason,
            String actorEmail) {
        CashRequestResponse requestView = requestService.findById(requestId, actorEmail);
        UserAccount actor = findActiveAccount(actorEmail);
        if (!canReview(actor, requestView)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le contrôle des justificatifs est réservé à la comptabilité.");
        }
        SupportingDocument document = documents.findByIdAndRequestId(documentId, requestId)
                .orElseThrow(() -> new CashRequestNotFoundException("Le justificatif est introuvable."));
        if (decision == SupportingDocumentStatus.ACCEPTE) {
            cashReturnService.assertDocumentAcceptance(requestId, document.getAmount());
        }
        Instant now = clock.instant();
        document.review(actor, decision, reason, now);
        recordEvent(document.getRequest(), actor,
                decision == SupportingDocumentStatus.ACCEPTE
                        ? "SUPPORTING_DOCUMENT_ACCEPTED" : "SUPPORTING_DOCUMENT_REJECTED",
                now);
        return SupportingDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public SupportingDocumentFile download(UUID requestId, UUID documentId, String viewerEmail) {
        requestService.findById(requestId, viewerEmail);
        SupportingDocument document = documents.findByIdAndRequestId(documentId, requestId)
                .orElseThrow(() -> new CashRequestNotFoundException("Le justificatif est introuvable."));
        if (!document.isEncrypted()) {
            throw new IllegalStateException("Le justificatif n’a pas été chiffré lors de la migration de stockage.");
        }
        return new SupportingDocumentFile(
                document.getFileName(), document.getMediaType(), crypto.decrypt(document.getFileData()));
    }

    private boolean canReview(UserAccount actor, CashRequestResponse request) {
        if (request.requesterId().equals(actor.getId())) return false;
        return actor.getRole() == AccountRole.COMPTABLE
                || (actor.getRole() == AccountRole.ADJOINT_COMPTABLE
                        && request.requesterRole() == AccountRole.COMPTABLE);
    }

    private UserAccount findActiveAccount(String email) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccountNotFoundException("Le compte connecté n’existe plus."));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le compte est désactivé.");
        }
        return account;
    }

    private String detectMediaType(byte[] file) {
        if (file.length >= 8 && file[0] == (byte) 0x89 && file[1] == 0x50
                && file[2] == 0x4e && file[3] == 0x47 && file[4] == 0x0d
                && file[5] == 0x0a && file[6] == 0x1a && file[7] == 0x0a) {
            return "image/png";
        }
        if (file.length >= 3 && file[0] == (byte) 0xff && file[1] == (byte) 0xd8
                && file[2] == (byte) 0xff) {
            return "image/jpeg";
        }
        if (file.length >= 5 && file[0] == '%' && file[1] == 'P' && file[2] == 'D'
                && file[3] == 'F' && file[4] == '-') {
            return "application/pdf";
        }
        throw new IllegalArgumentException("Format refusé : seuls les fichiers PDF, JPG et PNG sont acceptés.");
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "justificatif";
        String normalized = fileName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_");
        return baseName.length() <= 255 ? baseName : baseName.substring(baseName.length() - 255);
    }

    private void recordEvent(CashRequest request, UserAccount actor, String action, Instant at) {
        events.save(new CashRequestEvent(
                UUID.randomUUID(),
                request.getId(),
                actor.getId(),
                action,
                request.getStatus().name(),
                request.getStatus().name(),
                null,
                at));
    }

    public record SupportingDocumentFile(String fileName, String mediaType, byte[] content) {
    }
}
