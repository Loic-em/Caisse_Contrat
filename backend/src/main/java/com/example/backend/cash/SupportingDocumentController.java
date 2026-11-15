package com.example.backend.cash;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cash-requests/{requestId}/supporting-documents")
public class SupportingDocumentController {

    private final SupportingDocumentService service;

    public SupportingDocumentController(SupportingDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupportingDocumentResponse> list(
            @PathVariable UUID requestId,
            Authentication authentication) {
        return service.list(requestId, authentication.getName());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupportingDocumentResponse> upload(
            @PathVariable UUID requestId,
            @RequestParam String documentType,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate documentDate,
            @RequestParam(required = false) UUID replacesId,
            @RequestParam MultipartFile file,
            Authentication authentication) {
        return ResponseEntity.status(201).body(service.upload(
                requestId, documentType, amount, documentDate, replacesId, file, authentication.getName()));
    }

    @PutMapping("/{documentId}/review")
    public SupportingDocumentResponse review(
            @PathVariable UUID requestId,
            @PathVariable UUID documentId,
            @RequestParam @NotNull SupportingDocumentStatus decision,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        return service.review(requestId, documentId, decision, reason, authentication.getName());
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable UUID requestId,
            @PathVariable UUID documentId,
            Authentication authentication) {
        SupportingDocumentService.SupportingDocumentFile file =
                service.download(requestId, documentId, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(file.content()));
    }
}
