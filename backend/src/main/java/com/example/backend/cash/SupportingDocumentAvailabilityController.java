package com.example.backend.cash;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supporting-documents")
public class SupportingDocumentAvailabilityController {

    private final AntivirusScanner antivirusScanner;
    private final SupportingDocumentCrypto crypto;

    public SupportingDocumentAvailabilityController(
            AntivirusScanner antivirusScanner,
            SupportingDocumentCrypto crypto) {
        this.antivirusScanner = antivirusScanner;
        this.crypto = crypto;
    }

    @GetMapping("/availability")
    public Map<String, Boolean> availability() {
        return Map.of("uploadsEnabled", antivirusScanner.isOperational() && crypto.isConfigured());
    }
}
