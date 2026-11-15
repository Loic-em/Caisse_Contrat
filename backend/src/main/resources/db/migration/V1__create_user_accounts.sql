CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    employee_id VARCHAR(64) NOT NULL UNIQUE,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    department VARCHAR(160) NOT NULL,
    job_title VARCHAR(160) NOT NULL,
    role VARCHAR(32),
    manager_n1_id UUID,
    manager_n2_id UUID,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    CONSTRAINT fk_user_accounts_manager_n1
        FOREIGN KEY (manager_n1_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_user_accounts_manager_n2
        FOREIGN KEY (manager_n2_id) REFERENCES user_accounts (id)
);
