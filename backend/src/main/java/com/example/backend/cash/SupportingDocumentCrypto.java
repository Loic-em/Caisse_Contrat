package com.example.backend.cash;

import com.example.backend.documents.AesGcmDocumentCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SupportingDocumentCrypto {

    private final AesGcmDocumentCipher cipher;

    public SupportingDocumentCrypto(@Value("${app.cash.documents.encryption-key:}") String configuredKey) {
        this.cipher = new AesGcmDocumentCipher(configuredKey, "APP_CASH_DOCUMENTS_ENCRYPTION_KEY");
    }

    public boolean isConfigured() {
        return cipher.isConfigured();
    }

    public byte[] encrypt(byte[] content) {
        return cipher.encrypt(content);
    }

    public byte[] decrypt(byte[] encrypted) {
        return cipher.decrypt(encrypted);
    }
}
