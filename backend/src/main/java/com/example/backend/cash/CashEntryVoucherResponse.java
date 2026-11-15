package com.example.backend.cash;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public record CashEntryVoucherResponse(
        UUID id,
        UUID requestId,
        UUID sourceVoucherId,
        String sourceVoucherNumber,
        String voucherNumber,
        String beneficiaryName,
        BigDecimal amount,
        String amountInWords,
        String reason,
        LocalDate returnedOn,
        CashFundOrigin fundOrigin,
        UUID issuedById,
        String issuedByName,
        Instant issuedAt,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        UUID authorizationId,
        List<Visa> visas) {

    public static CashEntryVoucherResponse from(CashEntryVoucher voucher) {
        var issuer = voucher.getIssuedBy();
        return new CashEntryVoucherResponse(
                voucher.getId(),
                voucher.getRequest().getId(),
                voucher.getSourceVoucher().getId(),
                voucher.getSourceVoucher().getVoucherNumber(),
                voucher.getVoucherNumber(),
                voucher.getBeneficiaryName(),
                voucher.getAmount(),
                voucher.getAmountInWords(),
                voucher.getReason(),
                voucher.getReturnedOn(),
                voucher.getFundOrigin(),
                issuer.getId(),
                issuer.getFirstName() + " " + issuer.getLastName(),
                voucher.getIssuedAt(),
                voucher.getBalanceBefore(),
                voucher.getBalanceAfter(),
                voucher.getAuthorization() == null ? null : voucher.getAuthorization().getId(),
                voucher.getVisas().stream().map(Visa::from).toList());
    }

    public record Visa(
            UUID id,
            CashVoucherVisaType type,
            UUID signerId,
            String signerName,
            Instant signedAt,
            Instant reauthenticatedAt,
            String signatureDataUrl) {

        static Visa from(CashEntryVoucherVisa visa) {
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
