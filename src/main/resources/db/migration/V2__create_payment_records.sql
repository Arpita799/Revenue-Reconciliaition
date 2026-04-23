CREATE TABLE payment_records (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    reference_id VARCHAR(255) NOT NULL,
    record_date DATE NOT NULL,
    paid_amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(255),
    is_duplicate BOOLEAN,
    source_file VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payment_billing
     FOREIGN KEY (reference_id)
         REFERENCES billing_records(invoice_id)
         ON DELETE RESTRICT
);