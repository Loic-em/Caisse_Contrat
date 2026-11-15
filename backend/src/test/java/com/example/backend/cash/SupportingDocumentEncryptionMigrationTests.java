package com.example.backend.cash;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

class SupportingDocumentEncryptionMigrationTests {

    @Test
    void encryptsExistingDocumentsWhenTheKeyIsConfigured() {
        SupportingDocumentRepository documents = mock(SupportingDocumentRepository.class);
        SupportingDocument document = mock(SupportingDocument.class);
        EntityManager entityManager = mock(EntityManager.class);
        byte[] source = "%PDF-existing".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        SupportingDocumentCrypto crypto = new SupportingDocumentCrypto(
                Base64.getEncoder().encodeToString(new byte[32]));
        when(documents.findTop10ByEncryptedFalseOrderByUploadedAtAsc()).thenReturn(List.of(document), List.of());
        when(document.getFileData()).thenReturn(source);

        new SupportingDocumentEncryptionMigration(documents, crypto, entityManager)
                .run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<byte[]> encrypted = ArgumentCaptor.forClass(byte[].class);
        verify(document).replaceWithEncryptedData(encrypted.capture());
        assertArrayEquals(source, crypto.decrypt(encrypted.getValue()));
        verify(documents).flush();
        verify(entityManager).clear();
    }

    @Test
    void refusesToStartWhenLegacyDocumentsCannotBeEncrypted() {
        SupportingDocumentRepository documents = mock(SupportingDocumentRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        when(documents.findTop10ByEncryptedFalseOrderByUploadedAtAsc())
                .thenReturn(List.of(mock(SupportingDocument.class)));
        SupportingDocumentCrypto crypto = new SupportingDocumentCrypto("");

        assertThrows(IllegalStateException.class, () -> new SupportingDocumentEncryptionMigration(
                documents, crypto, entityManager).run(new DefaultApplicationArguments(new String[0])));
    }
}
