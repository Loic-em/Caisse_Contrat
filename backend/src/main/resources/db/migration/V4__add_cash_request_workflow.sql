ALTER TABLE cash_requests ADD COLUMN requester_id UUID;
ALTER TABLE cash_requests ADD COLUMN validator_n1_id UUID;
ALTER TABLE cash_requests ADD COLUMN validator_n2_id UUID;
ALTER TABLE cash_requests ADD COLUMN final_amount DECIMAL(19, 2);
ALTER TABLE cash_requests ADD COLUMN amount_adjustment_reason VARCHAR(1000);
ALTER TABLE cash_requests ADD COLUMN rejection_reason VARCHAR(1000);
ALTER TABLE cash_requests ADD COLUMN cancellation_reason VARCHAR(1000);
ALTER TABLE cash_requests ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cash_requests ADD COLUMN n1_decision_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cash_requests ADD COLUMN n2_decision_at TIMESTAMP WITH TIME ZONE;

UPDATE cash_requests SET status = 'SOUMISE' WHERE status = 'PENDING_REVIEW';
UPDATE cash_requests SET status = 'VALIDEE' WHERE status = 'APPROVED';
UPDATE cash_requests SET status = 'A_CORRIGER' WHERE status = 'REJECTED';

ALTER TABLE cash_requests
    ADD CONSTRAINT fk_cash_requests_requester
        FOREIGN KEY (requester_id) REFERENCES user_accounts (id);
ALTER TABLE cash_requests
    ADD CONSTRAINT fk_cash_requests_validator_n1
        FOREIGN KEY (validator_n1_id) REFERENCES user_accounts (id);
ALTER TABLE cash_requests
    ADD CONSTRAINT fk_cash_requests_validator_n2
        FOREIGN KEY (validator_n2_id) REFERENCES user_accounts (id);

CREATE INDEX idx_cash_requests_requester ON cash_requests (requester_id);
CREATE INDEX idx_cash_requests_validators ON cash_requests (validator_n1_id, validator_n2_id);

CREATE TABLE cash_request_events (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    reason VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cash_request_events_request
        FOREIGN KEY (request_id) REFERENCES cash_requests (id),
    CONSTRAINT fk_cash_request_events_actor
        FOREIGN KEY (actor_id) REFERENCES user_accounts (id)
);

CREATE INDEX idx_cash_request_events_request_time
    ON cash_request_events (request_id, occurred_at);
