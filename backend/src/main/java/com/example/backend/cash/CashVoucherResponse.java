package com.example.backend.cash;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public record CashVoucherResponse(
        UUID id,
        UUID requestId,
        CashRequestStatus requestStatus,
        String voucherNumber,
        CashVoucherType type,
        CashVoucherStatus status,
        String beneficiaryName,
        String requestSubject,
        String missionLocation,
        LocalDate missionStartDate,
        LocalDate missionEndDate,
        BigDecimal amount,
        String amountInWords,
        String reason,
        String category,
        UUID issuedById,
        String issuedByName,
        Instant issuedAt,
        List<Visa> visas,
        LocalDate disbursedOn,
        CashFundOrigin fundOrigin,
        UUID disbursedById,
        String disbursedByName,
        Instant disbursedAt) {

    public static CashVoucherResponse from(CashVoucher voucher) {
        var issuer = voucher.getIssuedBy();
        return new CashVoucherResponse(
                voucher.getId(),
                voucher.getRequest().getId(),
                voucher.getRequest().getStatus(),
                voucher.getVoucherNumber(),
                voucher.getType(),
                voucher.getStatus(),
                voucher.getBeneficiaryName(),
                voucher.getRequest().getSubject(),
                voucher.getRequest().getLocation(),
                voucher.getRequest().getStartDate(),
                voucher.getRequest().getEndDate(),
                voucher.getAmount(),
                voucher.getAmountInWords(),
                voucher.getReason(),
                voucher.getCategory(),
                issuer.getId(),
                issuer.getFirstName() + " " + issuer.getLastName(),
                voucher.getIssuedAt(),
                voucher.getVisas().stream().map(Visa::from).toList(),
                voucher.getDisbursedOn(),
                voucher.getFundOrigin(),
                voucher.getDisbursedBy() == null ? null : voucher.getDisbursedBy().getId(),
                voucher.getDisbursedBy() == null
                        ? null
                        : voucher.getDisbursedBy().getFirstName() + " "
                                + voucher.getDisbursedBy().getLastName(),
                voucher.getDisbursedAt());
    }

    public record Visa(
            UUID id,
            CashVoucherVisaType type,
            UUID signerId,
            String signerName,
            Instant signedAt,
            Instant reauthenticatedAt,
            String signatureDataUrl) {

        static Visa from(CashVoucherVisa visa) {
            return new Visa(
                    visa.getId(),
                    visa.getType(),
                    visa.getSigner().getId(),
                    visa.getSigner().getFirstName() + " " + visa.getSigner().getLastName(),
                    visa.getSignedAt(),
                    visa.getReauthenticatedAt(),
                    "data:image/png;base64,"
                            + Base64.getEncoder().encodeToString(visa.getSignatureSnapshot()));
        }
    }
}
