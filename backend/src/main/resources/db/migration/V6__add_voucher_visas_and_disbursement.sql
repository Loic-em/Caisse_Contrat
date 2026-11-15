CREATE TABLE user_signatures (
    user_id UUID PRIMARY KEY,
    signature_png BYTEA NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_signatures_account
        FOREIGN KEY (user_id) REFERENCES user_accounts (id)
);

CREATE TABLE cash_voucher_visas (
    id UUID PRIMARY KEY,
    voucher_id UUID NOT NULL,
    visa_type VARCHAR(20) NOT NULL,
    signer_id UUID NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reauthenticated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signature_snapshot BYTEA NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_cash_voucher_visa_type UNIQUE (voucher_id, visa_type),
    CONSTRAINT uq_cash_voucher_visa_signer UNIQUE (voucher_id, signer_id),
    CONSTRAINT fk_cash_voucher_visas_voucher
        FOREIGN KEY (voucher_id) REFERENCES cash_vouchers (id),
    CONSTRAINT fk_cash_voucher_visas_signer
        FOREIGN KEY (signer_id) REFERENCES user_accounts (id),
    CONSTRAINT chk_cash_voucher_visa_type
        CHECK (visa_type IN ('DIRECTION', 'CAISSE', 'BENEFICIAIRE'))
);

ALTER TABLE cash_vouchers ADD COLUMN disbursed_on DATE;
ALTER TABLE cash_vouchers ADD COLUMN fund_origin VARCHAR(20);
ALTER TABLE cash_vouchers ADD COLUMN disbursed_by_id UUID;
ALTER TABLE cash_vouchers ADD COLUMN disbursed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cash_vouchers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE cash_vouchers
    ADD CONSTRAINT fk_cash_vouchers_disburser
        FOREIGN KEY (disbursed_by_id) REFERENCES user_accounts (id);

ALTER TABLE cash_vouchers
    ADD CONSTRAINT chk_cash_vouchers_fund_origin
        CHECK (fund_origin IS NULL OR fund_origin IN ('BNI', 'NSIA', 'BRIDGE_BANK', 'BGDI', 'BICICI', 'DG'));

ALTER TABLE cash_vouchers
    ADD CONSTRAINT chk_cash_vouchers_disbursement_fields
        CHECK (
            (status = 'EMIS' AND disbursed_on IS NULL AND fund_origin IS NULL
                AND disbursed_by_id IS NULL AND disbursed_at IS NULL)
            OR (status = 'DECAISSE' AND disbursed_on IS NOT NULL AND fund_origin IS NOT NULL
                AND disbursed_by_id IS NOT NULL AND disbursed_at IS NOT NULL)
        );
