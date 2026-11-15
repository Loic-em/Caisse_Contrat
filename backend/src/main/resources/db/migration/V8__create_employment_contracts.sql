CREATE TABLE employment_contracts (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    contract_type VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    job_title VARCHAR(160) NOT NULL,
    department VARCHAR(160) NOT NULL,
    probation_period VARCHAR(100),
    workplace VARCHAR(200),
    supervisor_id UUID NOT NULL,
    internal_notes VARCHAR(2000),
    created_by_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_employment_contracts_employee
        FOREIGN KEY (employee_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_employment_contracts_supervisor
        FOREIGN KEY (supervisor_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_employment_contracts_creator
        FOREIGN KEY (created_by_id) REFERENCES user_accounts (id),
    CONSTRAINT chk_employment_contract_type
        CHECK (contract_type IN ('STAGE', 'CDD', 'CDI')),
    CONSTRAINT chk_employment_contract_status
        CHECK (status IN ('BROUILLON', 'ACTIF', 'A_RENOUVELER', 'TERMINE', 'ROMPU')),
    CONSTRAINT chk_employment_contract_dates
        CHECK (
            (contract_type = 'CDI' AND end_date IS NULL)
            OR (contract_type IN ('STAGE', 'CDD') AND end_date > start_date)
        )
);

CREATE INDEX idx_employment_contracts_employee
    ON employment_contracts (employee_id, updated_at);

CREATE TABLE employment_contract_documents (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL,
    uploaded_by_id UUID NOT NULL,
    version INTEGER NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    file_data BYTEA NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_employment_contract_documents_contract
        FOREIGN KEY (contract_id) REFERENCES employment_contracts (id),
    CONSTRAINT fk_employment_contract_documents_uploader
        FOREIGN KEY (uploaded_by_id) REFERENCES user_accounts (id),
    CONSTRAINT uq_employment_contract_document_version
        UNIQUE (contract_id, version),
    CONSTRAINT chk_employment_contract_document_version
        CHECK (version > 0),
    CONSTRAINT chk_employment_contract_document_type
        CHECK (media_type = 'application/pdf')
);

CREATE TABLE employment_contract_events (
    id UUID PRIMARY KEY,
    contract_id UUID,
    actor_id UUID NOT NULL,
    action VARCHAR(48) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_employment_contract_events_contract
        FOREIGN KEY (contract_id) REFERENCES employment_contracts (id),
    CONSTRAINT fk_employment_contract_events_actor
        FOREIGN KEY (actor_id) REFERENCES user_accounts (id)
);

CREATE INDEX idx_employment_contract_events_contract
    ON employment_contract_events (contract_id, occurred_at);
