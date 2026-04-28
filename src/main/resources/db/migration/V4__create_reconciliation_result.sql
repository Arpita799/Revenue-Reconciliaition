CREATE TABLE reconciliation_result (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    invoice_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    billed_amount NUMERIC(15,2) NOT NULL,
    paid_amount NUMERIC(15,2) NOT NULL,
    difference NUMERIC(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes VARCHAR(255),
    reconciled_by VARCHAR(255),
    billing_date DATE,
    created_at TIMESTAMP,
    reconciled_at TIMESTAMP,

    CONSTRAINT chk_reconciliation_status
        CHECK (status IN ('MATCHED', 'PARTIAL', 'OVERPAID', 'UNPAID'))
);