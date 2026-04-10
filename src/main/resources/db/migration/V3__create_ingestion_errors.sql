CREATE TABLE ingestion_errors (
    id BIGSERIAL PRIMARY KEY,
    source_file VARCHAR(255) NOT NULL,
    row_number INTEGER NOT NULL,
    raw_line VARCHAR(2000) NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    file_type VARCHAR(255),
    created_at TIMESTAMP
);