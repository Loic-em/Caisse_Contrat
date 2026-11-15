package com.example.backend.accounts;

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
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false, unique = true, length = 64)
    private String employeeId;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 120)
    private String lastName;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 160)
    private String department;

    @Column(name = "job_title", nullable = false, length = 160)
    private String jobTitle;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AccountRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_n1_id")
    private UserAccount managerN1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_n2_id")
    private UserAccount managerN2;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserAccount() {
    }

    UserAccount(
            UUID id,
            String employeeId,
            String firstName,
            String lastName,
            String email,
            String department,
            String jobTitle,
            AccountRole role,
            UserAccount managerN1,
            UserAccount managerN2,
            Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        updateDetails(
                employeeId,
                firstName,
                lastName,
                email,
                department,
                jobTitle,
                role,
                managerN1,
                managerN2);
        this.active = true;
    }

    void updateDetails(
            String employeeId,
            String firstName,
            String lastName,
            String email,
            String department,
            String jobTitle,
            AccountRole role,
            UserAccount managerN1,
            UserAccount managerN2) {
        this.employeeId = normalizeRequired(employeeId, "L’identifiant interne");
        this.firstName = normalizeRequired(firstName, "Le prénom");
        this.lastName = normalizeRequired(lastName, "Le nom");
        this.email = normalizeRequired(email, "L’adresse e-mail").toLowerCase(Locale.ROOT);
        this.department = normalizeRequired(department, "Le service");
        this.jobTitle = normalizeRequired(jobTitle, "Le poste");
        this.role = role;
        this.managerN1 = managerN1;
        this.managerN2 = managerN2;
    }

    void deactivate(Instant at) {
        if (!active) {
            throw new IllegalStateException("Ce compte est déjà désactivé.");
        }
        active = false;
        deactivatedAt = at;
    }

    void reactivate() {
        if (active) {
            throw new IllegalStateException("Ce compte est déjà actif.");
        }
        active = true;
        deactivatedAt = null;
    }

    void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe chiffré est obligatoire.");
        }
        this.passwordHash = passwordHash;
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " est obligatoire.");
        }
        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountRole getRole() {
        return role;
    }

    public UserAccount getManagerN1() {
        return managerN1;
    }

    public UserAccount getManagerN2() {
        return managerN2;
    }

    public AccountStatus getStatus() {
        return active ? AccountStatus.ACTIVE : AccountStatus.DISABLED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public long getVersion() {
        return version;
    }
}
