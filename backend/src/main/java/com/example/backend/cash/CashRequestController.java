package com.example.backend.cash;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/cash-requests")
public class CashRequestController {

    private final CashRequestService service;
    private final CashVoucherService voucherService;
    private final CashReturnService cashReturnService;

    public CashRequestController(
            CashRequestService service,
            CashVoucherService voucherService,
            CashReturnService cashReturnService) {
        this.service = service;
        this.voucherService = voucherService;
        this.cashReturnService = cashReturnService;
    }

    @GetMapping
    public List<CashRequestResponse> list(Authentication authentication) {
        return service.findAll(authentication.getName());
    }

    @GetMapping("/{id}")
    public CashRequestResponse get(@PathVariable UUID id, Authentication authentication) {
        return service.findById(id, authentication.getName());
    }

    @PostMapping
    public ResponseEntity<CashRequestResponse> create(
            @Valid @RequestBody CashRequestPayload payload,
            Authentication authentication,
            UriComponentsBuilder uriBuilder) {
        CashRequestResponse response = service.create(payload, authentication.getName());
        URI location = uriBuilder.path("/api/cash-requests/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public CashRequestResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CashRequestPayload payload,
            Authentication authentication) {
        return service.update(id, payload, authentication.getName());
    }

    @PostMapping("/{id}/submit")
    public CashRequestResponse submit(@PathVariable UUID id, Authentication authentication) {
        return service.submit(id, authentication.getName());
    }

    @PostMapping("/{id}/approve")
    public CashRequestResponse approve(@PathVariable UUID id, Authentication authentication) {
        return service.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    public CashRequestResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody CashRequestReasonPayload payload,
            Authentication authentication) {
        return service.reject(id, payload.reason(), authentication.getName());
    }

    @PostMapping("/{id}/cancel")
    public CashRequestResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CashRequestReasonPayload payload,
            Authentication authentication) {
        return service.cancel(id, payload.reason(), authentication.getName());
    }

    @PostMapping("/{id}/accounting-review")
    public CashRequestResponse setFinalAmount(
            @PathVariable UUID id,
            @Valid @RequestBody CashRequestAccountingPayload payload,
            Authentication authentication) {
        return service.setFinalAmount(
                id,
                payload.finalAmount(),
                payload.reason(),
                authentication.getName());
    }

    @GetMapping("/{id}/voucher")
    public CashVoucherResponse getVoucher(@PathVariable UUID id, Authentication authentication) {
        return voucherService.findByRequestId(id, authentication.getName());
    }

    @PostMapping("/{id}/voucher")
    public ResponseEntity<CashVoucherResponse> issueVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody CashVoucherPayload payload,
            Authentication authentication,
            UriComponentsBuilder uriBuilder) {
        CashVoucherResponse response = voucherService.issue(id, payload.reason(), authentication.getName());
        URI location = uriBuilder.path("/api/cash-requests/{id}/voucher").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{id}/voucher/visa")
    public CashVoucherResponse signVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody CashVoucherVisaPayload payload,
            Authentication authentication,
            HttpSession session) {
        String actorEmail = authentication.getName();
        Object reauthenticatedBy = session.getAttribute("cashVisaReauthenticatedBy");
        Object reauthenticatedAtValue = session.getAttribute("cashVisaReauthenticatedAt");
        if (!actorEmail.equals(reauthenticatedBy) || !(reauthenticatedAtValue instanceof String value)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Confirmez votre identité avant d’apposer le visa.");
        }
        Instant reauthenticatedAt;
        try {
            reauthenticatedAt = Instant.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            session.removeAttribute("cashVisaReauthenticatedBy");
            session.removeAttribute("cashVisaReauthenticatedAt");
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "La confirmation d’identité a expiré.");
        }
        session.removeAttribute("cashVisaReauthenticatedBy");
        session.removeAttribute("cashVisaReauthenticatedAt");
        return voucherService.sign(id, payload.type(), actorEmail, reauthenticatedAt);
    }

    @PostMapping("/{id}/voucher/disbursement")
    public CashVoucherResponse disburseVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody CashDisbursementPayload payload,
            Authentication authentication) {
        return voucherService.disburse(
                id,
                payload.fundOrigin(),
                payload.disbursedOn(),
                authentication.getName());
    }

    @GetMapping("/{id}/regularization")
    public CashRegularizationResponse getRegularization(
            @PathVariable UUID id,
            Authentication authentication) {
        return cashReturnService.summary(id, authentication.getName());
    }

    @PostMapping("/{id}/entry-vouchers")
    public ResponseEntity<CashEntryVoucherResponse> issueEntryVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody CashEntryVoucherPayload payload,
            Authentication authentication) {
        return ResponseEntity.status(201).body(
                cashReturnService.issueEntryVoucher(id, payload, authentication.getName()));
    }

    @PostMapping("/{id}/entry-vouchers/authorizations")
    public ResponseEntity<CashReturnAuthorizationResponse> authorizeEntryVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody CashReturnAuthorizationPayload payload,
            Authentication authentication) {
        return ResponseEntity.status(201).body(cashReturnService.authorizePartialReturn(
                id, payload.amount(), payload.reason(), authentication.getName()));
    }

    @PostMapping("/{id}/entry-vouchers/{entryVoucherId}/visa")
    public CashEntryVoucherResponse signEntryVoucher(
            @PathVariable UUID id,
            @PathVariable UUID entryVoucherId,
            @Valid @RequestBody CashVoucherVisaPayload payload,
            Authentication authentication,
            HttpSession session) {
        String actorEmail = authentication.getName();
        Object reauthenticatedBy = session.getAttribute("cashVisaReauthenticatedBy");
        Object reauthenticatedAtValue = session.getAttribute("cashVisaReauthenticatedAt");
        if (!actorEmail.equals(reauthenticatedBy) || !(reauthenticatedAtValue instanceof String value)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Confirmez votre identité avant d’apposer le visa.");
        }
        Instant reauthenticatedAt;
        try {
            reauthenticatedAt = Instant.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            session.removeAttribute("cashVisaReauthenticatedBy");
            session.removeAttribute("cashVisaReauthenticatedAt");
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "La confirmation d’identité a expiré.");
        }
        session.removeAttribute("cashVisaReauthenticatedBy");
        session.removeAttribute("cashVisaReauthenticatedAt");
        return cashReturnService.signEntryVoucher(
                id, entryVoucherId, payload.type(), actorEmail, reauthenticatedAt);
    }

    @PostMapping("/{id}/regularization/close")
    public ResponseEntity<Void> closeRegularization(
            @PathVariable UUID id,
            Authentication authentication) {
        cashReturnService.closeRegularization(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
