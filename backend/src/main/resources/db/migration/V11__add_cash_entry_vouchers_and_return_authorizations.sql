CREATE TABLE cash_return_authorizations (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    authorized_amount DECIMAL(19, 2) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    authorized_by_id UUID NOT NULL,
    authorized_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cash_return_authorizations_request
        FOREIGN KEY (request_id) REFERENCES cash_requests (id),
    CONSTRAINT fk_cash_return_authorizations_authorizer
        FOREIGN KEY (authorized_by_id) REFERENCES user_accounts (id),
    CONSTRAINT chk_cash_return_authorizations_amount
        CHECK (authorized_amount > 0)
);

CREATE TABLE cash_entry_vouchers (
    id UUID PRIMARY KEY,
    voucher_number VARCHAR(32) NOT NULL UNIQUE,
    request_id UUID NOT NULL,
    source_voucher_id UUID NOT NULL,
    beneficiary_name VARCHAR(241) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    amount_in_words VARCHAR(512) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    returned_on DATE NOT NULL,
    issued_by_id UUID NOT NULL,
    authorization_id UUID UNIQUE,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    balance_before DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cash_entry_vouchers_request
        FOREIGN KEY (request_id) REFERENCES cash_requests (id),
    CONSTRAINT fk_cash_entry_vouchers_source
        FOREIGN KEY (source_voucher_id) REFERENCES cash_vouchers (id),
    CONSTRAINT fk_cash_entry_vouchers_issuer
        FOREIGN KEY (issued_by_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_cash_entry_vouchers_authorization
        FOREIGN KEY (authorization_id) REFERENCES cash_return_authorizations (id),
    CONSTRAINT chk_cash_entry_vouchers_balance
        CHECK (amount > 0 AND balance_before >= amount AND balance_after = balance_before - amount)
);

CREATE INDEX idx_cash_entry_vouchers_request
    ON cash_entry_vouchers (request_id, issued_at);

CREATE INDEX idx_cash_return_authorizations_request
    ON cash_return_authorizations (request_id, authorized_at);
