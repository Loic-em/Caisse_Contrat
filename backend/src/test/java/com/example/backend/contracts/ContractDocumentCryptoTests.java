package com.example.backend.contracts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ContractDocumentCryptoTests {

    private final ContractDocumentCrypto crypto =
            new ContractDocumentCrypto(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void encryptsDocumentsWithDifferentNoncesAndRestoresContent() {
        byte[] source = "%PDF-test contract".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] first = crypto.encrypt(source);
        byte[] second = crypto.encrypt(source);

        assertNotEquals(java.util.Arrays.toString(first), java.util.Arrays.toString(second));
        assertArrayEquals(source, crypto.decrypt(first));
        assertArrayEquals(source, crypto.decrypt(second));
    }

    @Test
    void refusesToEncryptWhenTheKeyIsMissingOrInvalidlySized() {
        ContractDocumentCrypto unconfigured = new ContractDocumentCrypto("");
        assertFalse(unconfigured.isConfigured());
        assertThrows(ResponseStatusException.class, () -> unconfigured.encrypt(new byte[] {1}));
    }

    @Test
    void rejectsTamperedEncryptedDocuments() {
        byte[] encrypted = crypto.encrypt("document".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        encrypted[encrypted.length - 1] ^= 1;

        assertThrows(IllegalStateException.class, () -> crypto.decrypt(encrypted));
    }
}
