# Revenue Reconciliation

A Spring Boot service that ingests billing and payment CSV files, persists them to PostgreSQL, and reconciles billing records against payments to produce a per-invoice status: `MATCHED`, `PARTIAL`, `OVERPAID`, or `UNPAID`.

Built as a portfolio project to demonstrate real-world backend engineering patterns: layered architecture, row-level error tracking for batch ingestion, transaction isolation for partial-failure recovery, and streaming CSV export for large result sets.

## Table of Contents

- [Problem this solves](#problem-this-solves)
- [Architecture](#architecture)
- [Key design decisions](#key-design-decisions)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [API reference](#api-reference)
- [CSV file formats](#csv-file-formats)
- [Reconciliation logic](#reconciliation-logic)
- [Testing](#testing)
- [Known limitations](#known-limitations)
- [Roadmap](#roadmap)

## Problem this solves

Any business that bills customers and receives payments separately (instead of point-of-sale capture) ends up with two independent data streams that drift apart: invoices that go unpaid, payments that come in short, payments that overshoot, and the occasional duplicate transaction from a retried webhook or a re-uploaded file. This service automates that reconciliation instead of doing it in a spreadsheet:

1. Upload a billing CSV and a payment CSV independently.
2. Each row is parsed, validated, and persisted; bad rows are logged with the reason instead of failing the whole file.
3. Run reconciliation (on demand or on a nightly schedule) to match payments against invoices and compute a status and a running difference.
4. Query or export the results.

## Architecture

Standard layered Spring MVC architecture — chosen deliberately over something like hexagonal/clean architecture, which would be over-engineering for a service this size.

```
src/main/java/com/arpita/reconciliation/
├── controller/        HTTP layer only — request/response mapping, no business logic
├── service/            Business logic: ingestion orchestration, reconciliation, persistence isolation
├── repository/          Spring Data JPA repositories
├── parser/              CSV row parsing for each file type
├── entity/              JPA entities
├── dto/                 Request/response records
├── enums/                Status enums (BillingStatus, ReconciliationStatus, FileType)
├── exception/            Custom exceptions + @RestControllerAdvice global handler
└── scheduler/             Cron-triggered reconciliation (thin trigger, no logic)
```

The rule enforced throughout: business logic always routes through the service layer. Controllers never call repositories directly, and the scheduler only calls the service — it contains no logic of its own.

## Key design decisions

These are the parts of the codebase that came out of debugging real failure modes, not just "make it work" code, and are the most interesting things to ask about in a code review.

**Per-row transaction isolation.** Each CSV row is persisted in its own `REQUIRES_NEW` transaction via `RecordPersistenceService`. Without this, a single duplicate-key violation on one row would mark the *entire* outer transaction as rollback-only — including the call that logs the error to `ingestion_errors` — so the failure would be silently swallowed instead of recorded. Isolating each row means one bad row gets logged and skipped while the rest of the file keeps processing.

**Two independent duplicate-detection strategies.** Whole-file re-upload detection and per-row duplicate detection solve different problems and are not redundant with each other:
- *Per-row*: a `HashSet` of all existing `transactionId`s is pre-loaded before processing a payment file, so a row whose transaction ID already exists anywhere in the database — even from a different file — is flagged `is_duplicate` and excluded from reconciliation totals.
- *Whole-file*: detecting that the exact same file was already uploaded (e.g., via filename or SHA-256 hash) is a separate, planned concern — see [Roadmap](#roadmap).

**Reconciliation always recomputes from source data.** `paidAmount` is summed fresh from `PaymentRecords` on every reconciliation run rather than read from a previously stored `ReconciliationResult`. This makes reconciliation idempotent and safe to re-run after new payments arrive for an already-processed invoice.

## Tech stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot 4.x |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL |
| Schema migrations | Flyway |
| Testing | JUnit 5, Mockito, MockMvc, AssertJ |
| Build | Maven |
| API testing | Postman collection (included in `postman/`) |

## Getting started

### Prerequisites

- Java 21
- PostgreSQL running locally (or accessible via connection string)
- Maven (or use the included `mvnw` wrapper)

### Setup

1. Create a database:
   ```sql
   CREATE DATABASE "RRDB";
   ```

2. Configure your local database credentials. **Do not commit real credentials** — use environment variables or a git-ignored `application-local.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/RRDB
   spring.datasource.username=${DB_USERNAME:postgres}
   spring.datasource.password=${DB_PASSWORD}
   ```

3. Run migrations and start the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Flyway runs migrations automatically on startup (`V1`–`V4`, covering all entities).

4. Run the test suite:
   ```bash
   ./mvnw test
   ```

### Importing the Postman collection

A full Postman collection covering happy-path and error-case uploads (invalid dates, invalid amounts, oversized files, wrong extensions, missing fields) is included under `postman/collections/`. Import it into Postman or use the [Postman CLI/VS Code extension](https://learning.postman.com/docs/getting-started/installation/) to run it directly against this repo's `postman/` folder.

## API reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/upload/billing` | Upload a billing CSV. Returns row-level success/failure counts. |
| `POST` | `/upload/payment` | Upload a payment CSV. Returns row-level success/failure counts. |
| `POST` | `/reconciliation/run` | Run reconciliation against all pending/partial billing records. |
| `GET` | `/reconciliation/results` | Paginated reconciliation results. Supports `page`, `size`, `status`, `accountId` filters. |
| `GET` | `/reconciliation/export` | Streams all reconciliation results as a CSV download. |

File uploads must be `.csv`, under 5 MB, and non-empty, or the request is rejected with a 400.

> Interactive OpenAPI/Swagger documentation is planned — see [Roadmap](#roadmap).

## CSV file formats

**Billing CSV** (`/upload/billing`):
```
accountId,recordDate,billedAmount,invoiceId
ACC001,2026-01-15,150.00,INV-001
```

**Payment CSV** (`/upload/payment`):
```
accountId,recordDate,paidAmount,transactionId,referenceId
ACC001,2026-01-20,150.00,TXN-001,INV-001
```

- `recordDate` must be ISO format (`YYYY-MM-DD`).
- Amount fields must be valid, non-negative decimals.
- `referenceId` in a payment row must match an existing `invoiceId` from a billing upload, or the row is rejected.
- Rows that fail validation are logged to `ingestion_errors` with the row number, raw line, and reason; the rest of the file continues processing.

## Reconciliation logic

For each pending or partially-paid invoice, all non-duplicate payments referencing it are summed and compared against the billed amount:

| Condition | Status |
|---|---|
| No payments found | `UNPAID` |
| `\|billed − paid\|` within tolerance (default `0.01`) | `MATCHED` |
| Paid exceeds billed | `OVERPAID` |
| Paid less than billed, outside tolerance | `PARTIAL` |

Tolerance is configurable via `reconciliation.match.tolerance` in `application.properties`. Reconciliation can be triggered manually (`POST /reconciliation/run`) or runs automatically on a configurable cron schedule (`reconciliation.schedule.cron`, default 2 AM nightly).

## Testing

The project favors fast, isolated unit tests with Mockito over a full integration suite (see [Roadmap](#roadmap) for planned Testcontainers-based integration tests). Current coverage:

- `BillingCsvParserTest`, `PaymentCsvParserTest` — row-level parsing and validation edge cases (missing fields, invalid dates/amounts, negative amounts, malformed columns)
- `IngestionServiceTest` — partial-failure handling, error logging, duplicate transaction detection
- `ReconciliationServiceTest` — all four status outcomes, duplicate-payment exclusion, idempotent re-reconciliation, CSV streaming export
- `UploadControllerTest` — HTTP-layer validation (file type, size, content)

Run with:
```bash
./mvnw test
```

## Known limitations

- **No authentication or authorization.** All endpoints are currently open. Not suitable for any environment with real financial data without adding access control first.
- **In-memory duplicate detection doesn't scale indefinitely.** `PaymentCsvParser` pre-loads all existing transaction IDs into a `HashSet` before processing a file — fine at moderate volume, but will need a streaming/paginated or DB-side approach for tables with tens of millions of rows.
- **Reconciliation loop performs per-invoice queries.** Some lookups inside the reconciliation loop (existing-result lookup, billing status update) happen per-record rather than batched, which limits throughput at very high invoice counts. Tracked for a batch-loading fix.
- No request rate limiting on upload endpoints.
- No OpenAPI/Swagger documentation yet.
- No CI pipeline yet — tests must currently be run locally.

## Roadmap

- [ ] README, API docs *(this file)*
- [ ] Swagger / OpenAPI via `springdoc-openapi-starter-webmvc-ui`
- [ ] `ReconciliationRun` audit table — track history of reconciliation runs
- [ ] Docker Compose for local PostgreSQL
- [ ] Spring Security (basic auth/API key, minimum)
- [ ] GitHub Actions CI running the test suite on every push/PR
- [ ] Batch-load fix for the reconciliation loop's per-invoice queries
- [ ] Whole-file re-upload detection (filename or SHA-256 hash)
- [ ] Testcontainers-based integration tests

## License

See [LICENSE](LICENSE).
