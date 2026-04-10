CREATE TABLE billing_records (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    invoice_id VARCHAR(255) NOT NULL UNIQUE,
    record_date DATE NOT NULL,
    billed_amount NUMERIC(15,2) NOT NULL,
    billing_status VARCHAR(255) NOT NULL,
    currency VARCHAR(255),
    source_file VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);