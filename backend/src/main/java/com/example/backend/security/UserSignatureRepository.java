package com.example.backend.security;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSignatureRepository extends JpaRepository<UserSignature, UUID> {
}
