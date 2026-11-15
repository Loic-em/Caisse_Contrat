ALTER TABLE cash_entry_vouchers
    ADD COLUMN fund_origin VARCHAR(20);

UPDATE cash_entry_vouchers entry_voucher
SET fund_origin = source_voucher.fund_origin
FROM cash_vouchers source_voucher
WHERE source_voucher.id = entry_voucher.source_voucher_id;

ALTER TABLE cash_entry_vouchers
    ALTER COLUMN fund_origin SET NOT NULL;

CREATE TABLE cash_entry_voucher_visas (
    id UUID PRIMARY KEY,
    entry_voucher_id UUID NOT NULL,
    visa_type VARCHAR(20) NOT NULL,
    signer_id UUID NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reauthenticated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signature_snapshot BYTEA NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cash_entry_voucher_visas_entry_voucher
        FOREIGN KEY (entry_voucher_id) REFERENCES cash_entry_vouchers (id),
    CONSTRAINT fk_cash_entry_voucher_visas_signer
        FOREIGN KEY (signer_id) REFERENCES user_accounts (id),
    CONSTRAINT uq_cash_entry_voucher_visa_type
        UNIQUE (entry_voucher_id, visa_type),
    CONSTRAINT uq_cash_entry_voucher_visa_signer
        UNIQUE (entry_voucher_id, signer_id)
);

CREATE INDEX idx_cash_entry_voucher_visas_entry_voucher
    ON cash_entry_voucher_visas (entry_voucher_id, signed_at);
