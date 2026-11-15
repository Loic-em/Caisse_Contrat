package com.example.backend.contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentContractRepository extends JpaRepository<EmploymentContract, UUID> {

    List<EmploymentContract> findAllByOrderByUpdatedAtDesc();

    List<EmploymentContract> findAllByEmployeeIdOrderByUpdatedAtDesc(UUID employeeId);

    Optional<EmploymentContract> findByIdAndEmployeeId(UUID id, UUID employeeId);
}
