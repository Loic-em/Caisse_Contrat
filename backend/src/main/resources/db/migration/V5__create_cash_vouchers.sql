CREATE TABLE cash_voucher_sequences (
    id BIGINT PRIMARY KEY,
    next_number BIGINT NOT NULL,
    CONSTRAINT chk_cash_voucher_sequence_next_number CHECK (next_number > 0)
);

INSERT INTO cash_voucher_sequences (id, next_number) VALUES (1, 1);

CREATE TABLE cash_vouchers (
    id UUID PRIMARY KEY,
    voucher_number VARCHAR(32) NOT NULL UNIQUE,
    type VARCHAR(16) NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    beneficiary_name VARCHAR(241) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    amount_in_words VARCHAR(512) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    category VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL,
    issued_by_id UUID NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cash_vouchers_request
        FOREIGN KEY (request_id) REFERENCES cash_requests (id),
    CONSTRAINT fk_cash_vouchers_issuer
        FOREIGN KEY (issued_by_id) REFERENCES user_accounts (id)
);
