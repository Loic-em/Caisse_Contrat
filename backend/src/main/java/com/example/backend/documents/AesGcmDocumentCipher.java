package com.example.backend.documents;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AesGcmDocumentCipher {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmDocumentCipher(String configuredKey, String environmentVariable) {
        byte[] decoded = new byte[0];
        if (configuredKey != null && !configuredKey.isBlank()) {
            try {
                decoded = Base64.getDecoder().decode(configuredKey.trim());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(environmentVariable + " doit être une clé Base64 valide.");
            }
        }
        this.key = decoded;
    }

    public boolean isConfigured() {
        return key.length == 32;
    }

    public byte[] encrypt(byte[] content) {
        requireKey();
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(content);
            byte[] result = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, result, 0, nonce.length);
            System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
            return result;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Le chiffrement du document a échoué.", exception);
        }
    }

    public byte[] decrypt(byte[] encrypted) {
        requireKey();
        if (encrypted == null || encrypted.length <= NONCE_LENGTH) {
            throw new IllegalStateException("Le document chiffré est invalide.");
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        byte[] content = new byte[encrypted.length - NONCE_LENGTH];
        System.arraycopy(encrypted, 0, nonce, 0, NONCE_LENGTH);
        System.arraycopy(encrypted, NONCE_LENGTH, content, 0, content.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return cipher.doFinal(content);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Le document est illisible ou sa clé de chiffrement est incorrecte.", exception);
        }
    }

    private void requireKey() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le dépôt est désactivé tant qu’une clé de chiffrement de 32 octets n’est pas configurée.");
        }
    }
}
