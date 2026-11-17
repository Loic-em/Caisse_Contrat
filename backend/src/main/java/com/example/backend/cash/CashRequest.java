package com.example.backend.cash;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.UserAccount;

@Entity
@Table(name = "cash_requests")
public class CashRequest {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, length = 160)
    private String location;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "estimated_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "activity_area", length = 160)
    private String activityArea;

    @Column(name = "activity_details", length = 160)
    private String activityDetails;

    @Column(name = "client_partner", length = 160)
    private String clientPartner;

    @Column(name = "mission_purpose", length = 2000)
    private String missionPurpose;

    @Column(name = "mission_reference", length = 120)
    private String missionReference;

    @Column(name = "contact_name", length = 160)
    private String contactName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private UserAccount requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validator_n1_id")
    private UserAccount validatorN1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validator_n2_id")
    private UserAccount validatorN2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CashRequestStatus status;

    @Column(name = "final_amount", precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "amount_adjustment_reason", length = 1000)
    private String amountAdjustmentReason;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "n1_decision_at")
    private Instant n1DecisionAt;

    @Column(name = "n2_decision_at")
    private Instant n2DecisionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected CashRequest() {
    }

    public CashRequest(
            UUID id,
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            UserAccount requester,
            Instant createdAt,
            Instant updatedAt) {
        this(id, subject, location, startDate, endDate, category, estimatedAmount,
                attachmentName, null, null, null, null, requester, createdAt, updatedAt);
    }

    public CashRequest(
            UUID id,
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            String missionPurpose,
            String missionReference,
            String contactName,
            String contactPhone,
            UserAccount requester,
            Instant createdAt,
            Instant updatedAt) {
        this(id, subject, location, startDate, endDate, category, estimatedAmount, attachmentName,
                missionPurpose, missionReference, contactName, contactPhone, null, null, null,
                requester, createdAt, updatedAt);
    }

    public CashRequest(
            UUID id,
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            String missionPurpose,
            String missionReference,
            String contactName,
            String contactPhone,
            String activityArea,
            String activityDetails,
            String clientPartner,
            UserAccount requester,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.requester = requester;
        this.status = CashRequestStatus.DRAFT;
        this.createdAt = createdAt;
        updateDetails(subject, location, startDate, endDate, category, estimatedAmount,
                attachmentName, missionPurpose, missionReference, contactName, contactPhone,
                activityArea, activityDetails, clientPartner, updatedAt);
    }

    public void updateDetails(
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            Instant updatedAt) {
        this.subject = normalizeRequired(subject, "L’objet de la mission");
        this.location = normalizeRequired(location, "Le lieu de la mission");
        this.startDate = validateDate(startDate, "La date de début");
        this.endDate = validateDate(endDate, "La date de fin");
        if (this.endDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("La date de fin doit être supérieure ou égale à la date de début.");
        }
        this.category = normalizeRequired(category, "La catégorie de dépense");
        if (estimatedAmount == null || estimatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant estimé doit être supérieur à zéro.");
        }
        this.estimatedAmount = estimatedAmount;
        this.attachmentName = attachmentName == null || attachmentName.isBlank() ? null : attachmentName.trim();
        this.updatedAt = updatedAt;
    }

    public void updateDetails(
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            String missionPurpose,
            String missionReference,
            String contactName,
            String contactPhone,
            Instant updatedAt) {
        updateDetails(subject, location, startDate, endDate, category, estimatedAmount, attachmentName,
                missionPurpose, missionReference, contactName, contactPhone, null, null, null, updatedAt);
    }

    public void updateDetails(
            String subject,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            BigDecimal estimatedAmount,
            String attachmentName,
            String missionPurpose,
            String missionReference,
            String contactName,
            String contactPhone,
            String activityArea,
            String activityDetails,
            String clientPartner,
            Instant updatedAt) {
        updateDetails(subject, location, startDate, endDate, category, estimatedAmount, attachmentName, updatedAt);
        this.missionPurpose = normalizeOptional(missionPurpose, "Les details complementaires", 2000);
        this.missionReference = normalizeOptional(missionReference, "La reference de mission", 120);
        this.contactName = normalizeOptional(contactName, "Le contact sur place", 160);
        this.contactPhone = normalizeOptional(contactPhone, "Le telephone du contact", 50);
        this.activityArea = normalizeOptional(activityArea, "L'activite DIGIN concernee", 160);
        this.activityDetails = normalizeOptional(activityDetails, "Le detail de l'activite", 160);
        this.clientPartner = normalizeOptional(clientPartner, "Le client ou partenaire", 160);
    }

    public void submit(UserAccount managerN1, UserAccount managerN2, Instant at) {
        if (status != CashRequestStatus.DRAFT && status != CashRequestStatus.A_CORRIGER) {
            throw new IllegalArgumentException("Seuls un brouillon ou une demande a corriger peuvent etre soumis.");
        }
        if (managerN1 == null) {
            throw new IllegalArgumentException("Le responsable N+1 du demandeur n'est pas defini.");
        }
        if (requester == null || requester.getId().equals(managerN1.getId())) {
            throw new IllegalArgumentException("Le demandeur ne peut pas etre son propre valideur N+1.");
        }
        if (managerN1.getRole() != AccountRole.DIRECTEUR_GENERAL && managerN2 == null) {
            throw new IllegalArgumentException("Le responsable N+2 du demandeur n'est pas defini.");
        }
        if (managerN2 != null && (managerN2.getId().equals(requester.getId())
                || managerN2.getId().equals(managerN1.getId()))) {
            throw new IllegalArgumentException("Les responsables N+1 et N+2 doivent etre distincts du demandeur et entre eux.");
        }
        this.validatorN1 = managerN1;
        this.validatorN2 = managerN1.getRole() == AccountRole.DIRECTEUR_GENERAL ? null : managerN2;
        this.n1DecisionAt = null;
        this.n2DecisionAt = null;
        this.rejectionReason = null;
        this.finalAmount = null;
        this.amountAdjustmentReason = null;
        this.status = CashRequestStatus.SOUMISE;
        this.submittedAt = at;
        this.updatedAt = at;
    }

    public void approveN1(Instant at) {
        requireStatus(CashRequestStatus.SOUMISE);
        this.n1DecisionAt = at;
        this.status = validatorN1.getRole() == AccountRole.DIRECTEUR_GENERAL
                ? CashRequestStatus.VALIDEE
                : CashRequestStatus.VALIDEE_N1;
        this.updatedAt = at;
    }

    public void approveN2(Instant at) {
        requireStatus(CashRequestStatus.VALIDEE_N1);
        this.n2DecisionAt = at;
        this.status = CashRequestStatus.VALIDEE;
        this.updatedAt = at;
    }

    public void reject(String reason, Instant at) {
        if (status != CashRequestStatus.SOUMISE && status != CashRequestStatus.VALIDEE_N1) {
            throw new IllegalArgumentException("Cette demande n’attend pas une décision hiérarchique.");
        }
        this.rejectionReason = normalizeReason(reason, "Le motif du rejet");
        this.status = CashRequestStatus.A_CORRIGER;
        this.updatedAt = at;
    }

    public void cancel(String reason, Instant at) {
        if (status != CashRequestStatus.DRAFT
                && status != CashRequestStatus.SOUMISE
                && status != CashRequestStatus.VALIDEE_N1
                && status != CashRequestStatus.A_CORRIGER) {
            throw new IllegalArgumentException("La demande ne peut plus être annulée à cette étape.");
        }
        this.cancellationReason = normalizeReason(reason, "Le motif de l’annulation");
        this.status = CashRequestStatus.ANNULEE;
        this.updatedAt = at;
    }

    public void setFinalAmount(BigDecimal amount, String reason, Instant at) {
        requireStatus(CashRequestStatus.VALIDEE);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant définitif doit être supérieur à zéro.");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Le montant définitif doit être un nombre entier.");
        }
        if (amount.compareTo(estimatedAmount) != 0 && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Un motif est obligatoire lorsque le montant change.");
        }
        this.finalAmount = amount;
        this.amountAdjustmentReason = reason == null || reason.isBlank() ? null : reason.trim();
        this.status = CashRequestStatus.MONTANT_ARRETE;
        this.updatedAt = at;
    }

    public void markVoucherIssued(Instant at) {
        requireStatus(CashRequestStatus.MONTANT_ARRETE);
        this.status = CashRequestStatus.BON_EMIS;
        this.updatedAt = at;
    }

    public void markDisbursed(Instant at) {
        requireStatus(CashRequestStatus.BON_EMIS);
        this.status = CashRequestStatus.DECAISSEE;
        this.updatedAt = at;
    }

public void markInRegularization(LocalDate today, Instant at) {
        requireStatus(CashRequestStatus.DECAISSEE);
        if (!endDate.isBefore(today)) {
            throw new IllegalArgumentException("La mission n'est pas encore terminée.");
        }
        this.status = CashRequestStatus.EN_REGULARISATION;
        this.updatedAt = at;
    }

    public void closeRegularization(Instant at) {
        requireStatus(CashRequestStatus.EN_REGULARISATION);
        this.status = CashRequestStatus.REGULARISEE;
        this.updatedAt = at;
    }

    private void requireStatus(CashRequestStatus expected) {
        if (status != expected) {
            throw new IllegalArgumentException("La demande n’est pas à cette étape du circuit.");
        }
    }

    private String normalizeReason(String reason, String label) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(label + " est obligatoire.");
        }
        return reason.trim();
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " est obligatoire.");
        }
        return value.trim();
    }

    private String normalizeOptional(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " ne peut pas d?passer " + maxLength + " caract?res.");
        }
        return normalized;
    }

    private LocalDate validateDate(LocalDate value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " est obligatoire.");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public String getActivityArea() {
        return activityArea;
    }

    public String getActivityDetails() {
        return activityDetails;
    }

    public String getClientPartner() {
        return clientPartner;
    }

    public String getMissionPurpose() {
        return missionPurpose;
    }

    public String getMissionReference() {
        return missionReference;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public UserAccount getRequester() {
        return requester;
    }

    public UserAccount getValidatorN1() {
        return validatorN1;
    }

    public UserAccount getValidatorN2() {
        return validatorN2;
    }

    public CashRequestStatus getStatus() {
        return status;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public String getAmountAdjustmentReason() {
        return amountAdjustmentReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getN1DecisionAt() {
        return n1DecisionAt;
    }

    public Instant getN2DecisionAt() {
        return n2DecisionAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
