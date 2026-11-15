package com.example.backend.contracts;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContractResponse> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @GetMapping("/employees")
    public List<ContractEmployeeResponse> employees(Authentication authentication) {
        return service.employees(authentication.getName());
    }

    @GetMapping("/supervisors")
    public List<ContractEmployeeResponse> supervisors(Authentication authentication) {
        return service.supervisors(authentication.getName());
    }

    @GetMapping("/document-upload-availability")
    public ContractDocumentAvailability documentAvailability() {
        return service.documentAvailability();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse create(@Valid @RequestBody ContractPayload payload, Authentication authentication) {
        return service.create(payload, authentication.getName());
    }

    @GetMapping("/{contractId}")
    public ContractResponse get(@PathVariable UUID contractId, Authentication authentication) {
        return service.get(contractId, authentication.getName());
    }

    @PutMapping("/{contractId}")
    public ContractResponse update(
            @PathVariable UUID contractId,
            @Valid @RequestBody ContractPayload payload,
            Authentication authentication) {
        return service.update(contractId, payload, authentication.getName());
    }

    @GetMapping("/{contractId}/documents")
    public List<ContractDocumentResponse> documents(
            @PathVariable UUID contractId,
            Authentication authentication) {
        return service.documents(contractId, authentication.getName());
    }

    @PostMapping(path = "/{contractId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ContractDocumentResponse uploadDocument(
            @PathVariable UUID contractId,
            @RequestParam MultipartFile file,
            Authentication authentication) {
        return service.uploadDocument(contractId, file, authentication.getName());
    }

    @GetMapping("/{contractId}/documents/{documentId}/file")
    public ResponseEntity<ByteArrayResource> downloadDocument(
            @PathVariable UUID contractId,
            @PathVariable UUID documentId,
            Authentication authentication) {
        ContractService.ContractFile file = service.downloadDocument(
                contractId, documentId, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(file.content()));
    }

    @GetMapping("/{contractId}/history")
    public List<Map<String, Object>> history(
            @PathVariable UUID contractId,
            Authentication authentication) {
        return service.history(contractId, authentication.getName()).stream()
                .map(event -> Map.<String, Object>of(
                        "action", event.getAction(),
                        "occurredAt", event.getOccurredAt(),
                        "actorId", event.getActorId()))
                .toList();
    }
}
