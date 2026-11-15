package com.example.backend.contracts;

import com.example.backend.accounts.AccountNotFoundException;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.AccountStatus;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import com.example.backend.cash.AntivirusScanner;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContractService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final EmploymentContractRepository contracts;
    private final ContractDocumentRepository documents;
    private final ContractEventRepository events;
    private final UserAccountRepository accounts;
    private final AntivirusScanner antivirusScanner;
    private final ContractDocumentCrypto crypto;
    private final Clock clock;

    public ContractService(
            EmploymentContractRepository contracts,
            ContractDocumentRepository documents,
            ContractEventRepository events,
            UserAccountRepository accounts,
            AntivirusScanner antivirusScanner,
            ContractDocumentCrypto crypto) {
        this.contracts = contracts;
        this.documents = documents;
        this.events = events;
        this.accounts = accounts;
        this.antivirusScanner = antivirusScanner;
        this.crypto = crypto;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public List<ContractResponse> list(String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requireContractAccess(actor);
        List<EmploymentContract> visibleContracts = canManageAny(actor)
                ? contracts.findAllByOrderByUpdatedAtDesc()
                : contracts.findAllByEmployeeIdOrderByUpdatedAtDesc(actor.getId());
        recordEvent(null, actor, "CONTRACT_LIST_VIEWED");
        return visibleContracts.stream().map(contract -> toResponse(contract, actor)).toList();
    }

    @Transactional
    public ContractResponse get(UUID contractId, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        EmploymentContract contract = findAuthorizedContract(contractId, actor);
        recordEvent(contract.getId(), actor, "CONTRACT_VIEWED");
        return toResponse(contract, actor);
    }

    @Transactional
    public List<ContractEmployeeResponse> employees(String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requireManagerAccess(actor);
        return accounts.findAll(Sort.by("lastName", "firstName")).stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .filter(account -> account.getRole() != AccountRole.ADMINISTRATEUR)
                .filter(account -> canEditEmployee(actor, account))
                .map(ContractEmployeeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContractEmployeeResponse> supervisors(String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requireManagerAccess(actor);
        return accounts.findAll(Sort.by("lastName", "firstName")).stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .filter(account -> account.getRole() != AccountRole.ADMINISTRATEUR)
                .map(ContractEmployeeResponse::from)
                .toList();
    }

    @Transactional
    public ContractResponse create(ContractPayload payload, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requireManagerAccess(actor);
        validatePayload(payload);
        UserAccount employee = findActiveEmployee(payload.employeeId());
        requireCanEditEmployee(actor, employee);
        UserAccount supervisor = resolveSupervisor(payload.supervisorId(), employee);
        Instant now = clock.instant();
        EmploymentContract contract = new EmploymentContract(
                UUID.randomUUID(),
                employee,
                payload.type(),
                payload.startDate(),
                payload.endDate(),
                payload.jobTitle(),
                payload.department(),
                payload.probationPeriod(),
                payload.workplace(),
                supervisor,
                payload.internalNotes(),
                actor.getId(),
                now);
        contracts.save(contract);
        recordEvent(contract.getId(), actor, "CONTRACT_CREATED");
        return toResponse(contract, actor);
    }

    @Transactional
    public ContractResponse update(UUID contractId, ContractPayload payload, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        requireManagerAccess(actor);
        validatePayload(payload);
        EmploymentContract contract = contracts.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Le contrat est introuvable."));
        requireCanEditEmployee(actor, contract.getEmployee());
        if (!contract.getEmployee().getId().equals(payload.employeeId())) {
            throw new IllegalArgumentException("L’employé rattaché à un contrat ne peut pas être remplacé.");
        }
        UserAccount supervisor = resolveSupervisor(payload.supervisorId(), contract.getEmployee());
        contract.update(
                payload.type(),
                payload.startDate(),
                payload.endDate(),
                payload.jobTitle(),
                payload.department(),
                payload.probationPeriod(),
                payload.workplace(),
                supervisor,
                payload.internalNotes(),
                clock.instant());
        recordEvent(contract.getId(), actor, "CONTRACT_UPDATED");
        return toResponse(contract, actor);
    }

    @Transactional
    public List<ContractDocumentResponse> documents(UUID contractId, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        EmploymentContract contract = findAuthorizedContract(contractId, actor);
        recordEvent(contract.getId(), actor, "CONTRACT_DOCUMENTS_VIEWED");
        return documents.findAllByContractIdOrderByVersionDesc(contractId).stream()
                .map(ContractDocumentResponse::from)
                .toList();
    }

    @Transactional
    public ContractDocumentResponse uploadDocument(
            UUID contractId,
            MultipartFile file,
            String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        EmploymentContract contract = contracts.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Le contrat est introuvable."));
        requireCanEditEmployee(actor, contract.getEmployee());
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le document PDF signé est obligatoire.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le document ne peut pas dépasser 10 Mo.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Impossible de lire le document transmis.", exception);
        }
        if (!isPdf(content)) {
            throw new IllegalArgumentException("Seuls les documents PDF sont acceptés.");
        }
        antivirusScanner.scan(content);
        byte[] encryptedContent = crypto.encrypt(content);
        List<ContractDocument> existing = documents.findAllByContractIdOrderByVersionDesc(contractId);
        ContractDocument document = documents.save(new ContractDocument(
                UUID.randomUUID(),
                contract,
                actor,
                existing.isEmpty() ? 1 : existing.getFirst().getVersion() + 1,
                safeFileName(file.getOriginalFilename()),
                "application/pdf",
                encryptedContent,
                clock.instant()));
        contract.activate();
        recordEvent(contract.getId(), actor, "CONTRACT_DOCUMENT_UPLOADED");
        return ContractDocumentResponse.from(document);
    }

    @Transactional
    public ContractFile downloadDocument(UUID contractId, UUID documentId, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        findAuthorizedContract(contractId, actor);
        ContractDocument document = documents.findByIdAndContractId(documentId, contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Le document est introuvable."));
        recordEvent(contractId, actor, "CONTRACT_DOCUMENT_DOWNLOADED");
        return new ContractFile(
                document.getFileName(),
                document.getMediaType(),
                crypto.decrypt(document.getFileData()));
    }

    @Transactional
    public List<ContractEvent> history(UUID contractId, String actorEmail) {
        UserAccount actor = findActiveAccount(actorEmail);
        EmploymentContract contract = findAuthorizedContract(contractId, actor);
        recordEvent(contract.getId(), actor, "CONTRACT_HISTORY_VIEWED");
        return events.findAllByContractIdOrderByOccurredAtDesc(contractId);
    }

    @Transactional(readOnly = true)
    public ContractDocumentAvailability documentAvailability() {
        return new ContractDocumentAvailability(crypto.isConfigured() && antivirusScanner.isOperational());
    }

    private EmploymentContract findAuthorizedContract(UUID contractId, UserAccount actor) {
        requireContractAccess(actor);
        EmploymentContract contract = contracts.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Le contrat est introuvable."));
        if (!canManageAny(actor) && !contract.getEmployee().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n’avez pas accès à ce contrat.");
        }
        return contract;
    }

    private ContractResponse toResponse(EmploymentContract contract, UserAccount actor) {
        UserAccount employee = contract.getEmployee();
        UserAccount supervisor = contract.getSupervisor();
        boolean showInternalNotes = actor.getRole() == AccountRole.COMPTABLE
                || actor.getRole() == AccountRole.DIRECTEUR_GENERAL;
        int documentCount = documents.findAllByContractIdOrderByVersionDesc(contract.getId()).size();
        return new ContractResponse(
                contract.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getRole(),
                contract.getType(),
                contract.getStatus(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getJobTitle(),
                contract.getDepartment(),
                contract.getProbationPeriod(),
                contract.getWorkplace(),
                supervisor.getId(),
                supervisor.getFirstName() + " " + supervisor.getLastName(),
                showInternalNotes ? contract.getInternalNotes() : null,
                documentCount,
                contract.getCreatedAt(),
                contract.getUpdatedAt());
    }

    private void requireContractAccess(UserAccount actor) {
        if (actor.getRole() == AccountRole.ADMINISTRATEUR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "L’administrateur n’a pas accès aux contrats.");
        }
        if (!canManageAny(actor) && actor.getRole() != AccountRole.ADJOINT_COMPTABLE
                && actor.getRole() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n’avez pas accès aux contrats.");
        }
    }

    private void requireManagerAccess(UserAccount actor) {
        if (actor.getRole() != AccountRole.COMPTABLE && actor.getRole() != AccountRole.DIRECTEUR_GENERAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La gestion des contrats est réservée à la comptabilité et à la Direction.");
        }
    }

    private boolean canManageAny(UserAccount actor) {
        return actor.getRole() == AccountRole.COMPTABLE || actor.getRole() == AccountRole.DIRECTEUR_GENERAL;
    }

    private void requireCanEditEmployee(UserAccount actor, UserAccount employee) {
        if (!canEditEmployee(actor, employee)) {
            if (actor.getRole() == AccountRole.COMPTABLE && employee.getId().equals(actor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le comptable ne peut pas gérer son propre contrat.");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas gérer le contrat de cet employé.");
        }
    }

    private boolean canEditEmployee(UserAccount actor, UserAccount employee) {
        if (actor.getRole() == AccountRole.COMPTABLE) {
            return !employee.getId().equals(actor.getId());
        }
        return actor.getRole() == AccountRole.DIRECTEUR_GENERAL
                && (employee.getRole() == AccountRole.COMPTABLE
                        || employee.getRole() == AccountRole.ADJOINT_COMPTABLE);
    }

    private UserAccount findActiveAccount(String email) {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccountNotFoundException("Le compte connecté n’existe plus."));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le compte est désactivé.");
        }
        return account;
    }

    private UserAccount findActiveEmployee(UUID employeeId) {
        UserAccount employee = accounts.findById(employeeId)
                .orElseThrow(() -> new AccountNotFoundException("L’employé sélectionné n’existe pas."));
        if (employee.getStatus() != AccountStatus.ACTIVE
                || employee.getRole() == AccountRole.ADMINISTRATEUR) {
            throw new IllegalArgumentException("Un contrat ne peut être rattaché qu’à un compte employé actif.");
        }
        return employee;
    }

    private UserAccount resolveSupervisor(UUID supervisorId, UserAccount employee) {
        UUID requestedId = supervisorId;
        if (requestedId == null && employee.getManagerN1() != null) {
            requestedId = employee.getManagerN1().getId();
        }
        if (requestedId == null) {
            throw new IllegalArgumentException("Le responsable est obligatoire et aucun N+1 n’est défini.");
        }
        UserAccount supervisor = accounts.findById(requestedId)
                .orElseThrow(() -> new AccountNotFoundException("Le responsable sélectionné n’existe pas."));
        if (supervisor.getStatus() != AccountStatus.ACTIVE || supervisor.getId().equals(employee.getId())) {
            throw new IllegalArgumentException("Le responsable sélectionné n’est pas valide.");
        }
        return supervisor;
    }

    private void validatePayload(ContractPayload payload) {
        if (payload == null || payload.employeeId() == null || payload.type() == null
                || payload.startDate() == null || payload.jobTitle() == null || payload.jobTitle().isBlank()
                || payload.department() == null || payload.department().isBlank()) {
            throw new IllegalArgumentException("Les champs obligatoires du contrat doivent être renseignés.");
        }
        boolean fixedTerm = payload.type() == ContractType.CDD || payload.type() == ContractType.STAGE;
        if (fixedTerm != (payload.endDate() != null)) {
            throw new IllegalArgumentException("La date de fin est obligatoire pour un CDD ou un stage et interdite pour un CDI.");
        }
        if (payload.endDate() != null && !payload.endDate().isAfter(payload.startDate())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }
        if (payload.internalNotes() != null && payload.internalNotes().length() > 2000) {
            throw new IllegalArgumentException("Les notes internes ne peuvent pas dépasser 2 000 caractères.");
        }
    }

    private boolean isPdf(byte[] content) {
        return content.length >= 5
                && content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-';
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "contrat.pdf";
        }
        String normalized = fileName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_");
        return baseName.length() <= 255 ? baseName : baseName.substring(baseName.length() - 255);
    }

    private void recordEvent(UUID contractId, UserAccount actor, String action) {
        events.save(new ContractEvent(UUID.randomUUID(), contractId, actor.getId(), action, clock.instant()));
    }

    public record ContractFile(String fileName, String mediaType, byte[] content) {
    }
}
