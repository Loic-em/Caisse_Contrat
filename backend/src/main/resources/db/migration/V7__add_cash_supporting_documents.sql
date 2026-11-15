CREATE TABLE cash_supporting_documents (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    uploaded_by_id UUID NOT NULL,
    replaces_id UUID,
    document_type VARCHAR(120) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    file_data BYTEA NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    document_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewed_by_id UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(1000),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cash_supporting_documents_request
        FOREIGN KEY (request_id) REFERENCES cash_requests (id),
    CONSTRAINT fk_cash_supporting_documents_uploader
        FOREIGN KEY (uploaded_by_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_cash_supporting_documents_replaces
        FOREIGN KEY (replaces_id) REFERENCES cash_supporting_documents (id),
    CONSTRAINT fk_cash_supporting_documents_reviewer
        FOREIGN KEY (reviewed_by_id) REFERENCES user_accounts (id),
    CONSTRAINT chk_cash_supporting_documents_status
        CHECK (status IN ('A_CONTROLER', 'ACCEPTE', 'REJETE')),
    CONSTRAINT chk_cash_supporting_documents_amount
        CHECK (amount > 0),
    CONSTRAINT chk_cash_supporting_documents_review
        CHECK (
            (status = 'A_CONTROLER' AND reviewed_by_id IS NULL AND reviewed_at IS NULL
                AND rejection_reason IS NULL)
            OR (status = 'ACCEPTE' AND reviewed_by_id IS NOT NULL AND reviewed_at IS NOT NULL
                AND rejection_reason IS NULL)
            OR (status = 'REJETE' AND reviewed_by_id IS NOT NULL AND reviewed_at IS NOT NULL
                AND rejection_reason IS NOT NULL)
        )
);

CREATE INDEX idx_cash_supporting_documents_request
    ON cash_supporting_documents (request_id, uploaded_at);
