package com.example.backend.accounts;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmployeeIdAndIdNot(String employeeId, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    boolean existsByRole(AccountRole role);

    boolean existsByRoleAndActiveTrue(AccountRole role);
    boolean existsByManagerN1_IdOrManagerN2_Id(UUID managerN1Id, UUID managerN2Id);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByEmployeeId(String employeeId);
}
