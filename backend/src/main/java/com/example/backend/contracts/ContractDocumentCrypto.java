package com.example.backend.contracts;

import com.example.backend.documents.AesGcmDocumentCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContractDocumentCrypto {

    private final AesGcmDocumentCipher cipher;

    public ContractDocumentCrypto(@Value("${app.contracts.encryption-key:}") String configuredKey) {
        this.cipher = new AesGcmDocumentCipher(configuredKey, "APP_CONTRACTS_ENCRYPTION_KEY");
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
