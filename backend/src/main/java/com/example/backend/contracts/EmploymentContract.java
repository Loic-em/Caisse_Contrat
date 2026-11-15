package com.example.backend.contracts;

import com.example.backend.accounts.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employment_contracts")
public class EmploymentContract {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private UserAccount employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 16)
    private ContractType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "job_title", nullable = false, length = 160)
    private String jobTitle;

    @Column(nullable = false, length = 160)
    private String department;

    @Column(name = "probation_period", length = 100)
    private String probationPeriod;

    @Column(length = 200)
    private String workplace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private UserAccount supervisor;

    @Column(name = "internal_notes", length = 2000)
    private String internalNotes;

    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected EmploymentContract() {
    }

    public EmploymentContract(
            UUID id,
            UserAccount employee,
            ContractType type,
            LocalDate startDate,
            LocalDate endDate,
            String jobTitle,
            String department,
            String probationPeriod,
            String workplace,
            UserAccount supervisor,
            String internalNotes,
            UUID createdById,
            Instant now) {
        this.id = id;
        this.employee = employee;
        this.status = ContractStatus.BROUILLON;
        this.createdById = createdById;
        this.createdAt = now;
        update(type, startDate, endDate, jobTitle, department, probationPeriod, workplace, supervisor, internalNotes, now);
    }

    public void update(
            ContractType type,
            LocalDate startDate,
            LocalDate endDate,
            String jobTitle,
            String department,
            String probationPeriod,
            String workplace,
            UserAccount supervisor,
            String internalNotes,
            Instant now) {
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.jobTitle = jobTitle.trim();
        this.department = department.trim();
        this.probationPeriod = normalizeOptional(probationPeriod);
        this.workplace = normalizeOptional(workplace);
        this.supervisor = supervisor;
        this.internalNotes = internalNotes == null || internalNotes.isBlank() ? null : internalNotes.trim();
        this.updatedAt = now;
    }

    public void activate() {
        if (status == ContractStatus.BROUILLON) {
            status = ContractStatus.ACTIF;
        }
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getEmployee() {
        return employee;
    }

    public ContractType getType() {
        return type;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public String getProbationPeriod() {
        return probationPeriod;
    }

    public String getWorkplace() {
        return workplace;
    }

    public UserAccount getSupervisor() {
        return supervisor;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
