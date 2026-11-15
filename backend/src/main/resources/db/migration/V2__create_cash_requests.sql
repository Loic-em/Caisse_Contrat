CREATE TABLE cash_requests (
    id UUID PRIMARY KEY,
    subject VARCHAR(160) NOT NULL,
    location VARCHAR(160) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    category VARCHAR(80) NOT NULL,
    estimated_amount DECIMAL(19, 2) NOT NULL,
    attachment_name VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
