package com.example.backend.cash;

import java.util.List;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SupportingDocumentEncryptionMigration implements ApplicationRunner {

    private final SupportingDocumentRepository documents;
    private final SupportingDocumentCrypto crypto;
    private final EntityManager entityManager;

    public SupportingDocumentEncryptionMigration(
            SupportingDocumentRepository documents,
            SupportingDocumentCrypto crypto,
            EntityManager entityManager) {
        this.documents = documents;
        this.crypto = crypto;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SupportingDocument> legacyDocuments = documents.findTop10ByEncryptedFalseOrderByUploadedAtAsc();
        if (!legacyDocuments.isEmpty() && !crypto.isConfigured()) {
            throw new IllegalStateException(
                    "Des justificatifs existants doivent être chiffrés. Configurez "
                            + "APP_CASH_DOCUMENTS_ENCRYPTION_KEY avant de démarrer le backend.");
        }
        while (!legacyDocuments.isEmpty()) {
            for (SupportingDocument document : legacyDocuments) {
                document.replaceWithEncryptedData(crypto.encrypt(document.getFileData()));
            }
            documents.flush();
            entityManager.clear();
            legacyDocuments = documents.findTop10ByEncryptedFalseOrderByUploadedAtAsc();
        }
    }
}
