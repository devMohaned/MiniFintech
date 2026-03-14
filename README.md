# MiniFintech Wallet App

## Important Disclaimer

This project is **not production-ready from a security standpoint**.

The most critical missing step is **authentication and authorization**. All business endpoints are currently accessible without identity verification or role-based access control. Before exposing this service beyond local development, add a proper security layer, preferably:

- **OAuth 2.0**
- **PKCE** for public clients
- Token validation on the API side
- Role and scope checks for wallet, transfer, and reversal operations

## Overview

`wallet-app` is a Spring Boot backend for a simplified fintech wallet system. It supports:

- Wallet creation
- Wallet balance credit and debit operations
- Wallet ledger retrieval
- Wallet-to-wallet transfers
- Transfer reversal
- Transfer idempotency
- Transactional outbox publishing to Kafka
- Dead-letter handling for failed event publication
- Request correlation IDs for traceability

The application persists data in PostgreSQL, uses Flyway for schema migrations, and publishes integration events to Kafka through the outbox pattern.

## Core Business Capabilities

### 1. Wallet management

Each wallet belongs to a `customerId` and a single `currency`. A customer cannot have more than one wallet for the same currency.

Implemented rules:

- Wallets are created with status `ACTIVE`
- Initial balance is `0.0000`
- Duplicate wallet per `customerId + currency` is rejected
- Credit/debit operations require currency match
- Debit is rejected when funds are insufficient

### 2. Ledger tracking

Every credit, debit, transfer, and reversal posts immutable ledger entries.

Ledger entry types:

- `CREDIT`
- `DEBIT`

Ledger reference types:

- `TOP_UP`
- `WITHDRAWAL`
- `TRANSFER`
- `REVERSAL`

### 3. Transfers

Transfers move money atomically from one wallet to another.

Implemented rules:

- Source and destination wallets must be different
- Both wallets must exist and be `ACTIVE`
- Transfer currency must match both wallets
- Source wallet must have enough funds
- Transfer starts as `PENDING` and is then marked `COMPLETED`
- A transfer-completed outbox event is stored after success

### 4. Reversals

Transfers can be reversed through a dedicated reversal endpoint.

Implemented rules:

- Only `COMPLETED` transfers are reversible
- A transfer can only be reversed once
- Destination wallet must still have enough funds to return the amount
- Reversal creates ledger entries and marks the original transfer as `REVERSED`
- A transfer-reversed outbox event is stored after success

### 5. Idempotency

Transfer creation requires the `Idempotency-Key` header.

Behavior:

- Same key + same payload returns the original completed transfer response
- Same key + different payload is rejected
- Duplicate in-flight requests are rejected
- Request body hash is generated using `SHA-256`

## Architecture

The application follows a layered structure:

- `controller`: REST endpoints
- `service`: business logic and transactional orchestration
- `repo`: Spring Data JPA repositories
- `domain`: JPA entities and enums
- `service/dto`: request and response contracts
- `service/mapper`: MapStruct mappers
- `common`: shared utilities, exceptions, and API error building
- `outbox`: event persistence and scheduled Kafka publication

High-level flow:

1. Client calls a REST endpoint.
2. Controller validates input and delegates to a service.
3. Service enforces business rules and persists changes transactionally.
4. Ledger entries are written for money movements.
5. Transfer and reversal flows create outbox records.
6. A scheduled publisher reads outbox records and sends them to Kafka.
7. Failed publications are retried with exponential backoff, then moved to a dead-letter table.

## Tech Stack

- Java 21
- Spring Boot 4.0.3
- Spring Web
- Spring Validation
- Spring Data JPA
- Spring Actuator
- Flyway
- PostgreSQL
- Apache Kafka
- MapStruct
- Lombok
- JUnit 5
- Testcontainers
- JaCoCo

## Repository Structure

```text
src/main/java/com/mini/fintech/wallet_app
  common/         Shared errors, utilities, factories
  config/         Scheduling and servlet filter config
  ledger/         Ledger domain, repo, service, mapper, DTOs
  outbox/         Outbox domain, service, publisher, repos, event DTOs
  reversal/       Reversal API, service, domain, repo, mapper, DTOs
  transfer/       Transfer API, service, domain, repo, mapper, DTOs
  wallet/         Wallet API, service, domain, repo, mapper, DTOs
  idemptotency/   Idempotency domain, service, repo, constants

src/main/resources
  application.yaml
  db/migration/   Flyway SQL migrations

src/test/java
  unit and integration tests

docker-compose.yml
infra/
```

## Configuration

Default local configuration from `src/main/resources/application.yaml`:

- HTTP port: `8080`
- PostgreSQL: `jdbc:postgresql://localhost:5432/fintech?currentSchema=wallet`
- DB username/password: `fintech` / `fintech`
- Kafka bootstrap server: `localhost:29092`
- Outbox batch size: `100`
- Outbox schedule delay: `5000 ms`
- Max publish attempts: `5`
- Topics:
  - `fintech.transfer.completed`
  - `fintech.transfer.reversed`

Actuator web endpoints exposed:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

## Local Setup

### Prerequisites

- Java 21
- Maven wrapper support (`mvnw.cmd` is included)
- Docker Desktop or compatible Docker runtime

### Start infrastructure

Run:

```powershell
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- Kafka on `localhost:29092`
- Kafka UI on `http://localhost:8085`

### Run the application

Using the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Or build and run:

```powershell
.\mvnw.cmd clean package
java -jar target\wallet-app-0.0.1-SNAPSHOT.jar
```

The API will start on:

```text
http://localhost:8080
```

### Run tests

```powershell
.\mvnw.cmd test
```

Integration tests use Testcontainers for PostgreSQL and Kafka.

## API Conventions

### Correlation ID

The filter accepts `X-Correlation-Id`. If the header is missing, the app generates one automatically and echoes it in the response.

This value is also included in error responses.

### Idempotency

`POST /api/v1/transfers` requires:

```text
Idempotency-Key: <unique-key>
```

Without it, transfer creation is rejected.

### Amount and currency rules

- Amount must be greater than `0.0000`
- Currency must be a 3-letter uppercase code such as `EGP`
- Monetary values are stored as `NUMERIC(19,4)`

## REST API

### 1. Create wallet

`POST /api/v1/wallets`

Headers:

- `Content-Type: application/json`
- `X-Correlation-Id: <optional>`

Request:

```json
{
  "customerId": "cust_123",
  "currency": "EGP"
}
```

Response:

```json
{
  "id": "uuid",
  "customerId": "cust_123",
  "currency": "EGP",
  "status": "ACTIVE",
  "availableBalance": 0.0000,
  "createdAt": "2026-03-14T12:00:00",
  "updatedAt": "2026-03-14T12:00:00"
}
```

### 2. Get wallet

`GET /api/v1/wallets/{walletId}`

Returns wallet details for the provided wallet ID.

### 3. Credit wallet

`POST /api/v1/wallets/{walletId}/credit`

Request:

```json
{
  "amount": "500.0000",
  "currency": "EGP",
  "reference": "manual_topup_001",
  "description": "Initial funding"
}
```

Behavior:

- Adds amount to wallet balance
- Creates a ledger entry with reference type `TOP_UP`

### 4. Debit wallet

`POST /api/v1/wallets/{walletId}/debit`

Request:

```json
{
  "amount": "200.0000",
  "currency": "EGP",
  "reference": "cash_out_001",
  "description": "ATM withdrawal simulation"
}
```

Behavior:

- Deducts amount from wallet balance
- Rejects when funds are insufficient
- Creates a ledger entry with reference type `WITHDRAWAL`

### 5. Get wallet ledger

`GET /api/v1/wallets/{walletId}/ledger?page=0&size=20`

Returns paginated ledger entries ordered by `createdAt DESC`.

Ledger item shape:

```json
{
  "id": "uuid",
  "transactionId": "uuid",
  "walletId": "uuid",
  "entryType": "CREDIT",
  "amount": 500.0000,
  "currency": "EGP",
  "referenceType": "TOP_UP",
  "referenceId": "manual_topup_001",
  "description": "Initial funding",
  "createdAt": "2026-03-14T12:05:00"
}
```

### 6. Create transfer

`POST /api/v1/transfers`

Headers:

- `Content-Type: application/json`
- `X-Correlation-Id: <optional>`
- `Idempotency-Key: <required>`

Request:

```json
{
  "sourceWalletId": "11111111-1111-1111-1111-111111111111",
  "destinationWalletId": "22222222-2222-2222-2222-222222222222",
  "amount": "150.0000",
  "currency": "EGP",
  "reason": "peer_transfer"
}
```

Response:

```json
{
  "transferId": "uuid",
  "sourceWalletId": "uuid",
  "destinationWalletId": "uuid",
  "amount": 150.0000,
  "currency": "EGP",
  "status": "COMPLETED",
  "reason": "peer_transfer",
  "createdAt": "2026-03-14T12:10:00",
  "completedAt": "2026-03-14T12:10:00"
}
```

Behavior:

- Creates one debit ledger entry on the source wallet
- Creates one credit ledger entry on the destination wallet
- Stores an outbox event for Kafka publication

### 7. Reverse transfer

`POST /api/v1/transfers/{transferId}/reversal`

Request:

```json
{
  "reason": "customer_dispute"
}
```

Response:

```json
{
  "reversalId": "uuid",
  "originalTransferId": "uuid",
  "status": "COMPLETED",
  "reason": "customer_dispute",
  "createdAt": "2026-03-14T12:20:00",
  "completedAt": "2026-03-14T12:20:00"
}
```

Behavior:

- Moves funds back from destination to source
- Marks original transfer as `REVERSED`
- Creates reversal ledger entries
- Stores a reversal outbox event for Kafka publication

## Error Handling

Error response format:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "currency must be a 3-letter uppercase code",
  "correlationId": "test-corr-001",
  "timestamp": "2026-03-14T12:30:00"
}
```

HTTP status behavior:

- `400 Bad Request`
  - validation failures
  - invalid currency
  - insufficient funds
  - missing idempotency key
  - invalid transfer input
- `404 Not Found`
  - wallet not found
  - transfer not found
- `409 Conflict`
  - wallet already exists
  - concurrent modification
  - idempotency key reuse with different request
  - request already in progress
  - transfer not reversible
- `500 Internal Server Error`
  - unexpected application failure

Key business error codes:

- `WALLET_ALREADY_EXISTS`
- `WALLET_INACTIVE`
- `INSUFFICIENT_FUNDS`
- `INVALID_CURRENCY`
- `INVALID_TRANSFER`
- `REQUEST_ALREADY_IN_PROGRESS`
- `CONCURRENT_MODIFICATION`
- `IDEMPOTENCY_KEY_REUSED`
- `IDEMPOTENCY_KEY_REQUIRED`
- `TRANSFER_NOT_REVERSIBLE`
- `REVERSAL_INSUFFICIENT_FUNDS`
- `TRANSFER_ALREADY_REVERSED`

## Database Model

Flyway creates the `wallet` schema and the following main tables:

### `wallet_accounts`

- wallet identity
- customer ownership
- currency
- status
- available balance
- optimistic locking via `version`

### `ledger_entries`

- immutable money movement journal
- references wallet, transaction, type, amount, and business reference

### `transfer_transactions`

- source wallet
- destination wallet
- amount and currency
- transfer status
- timestamps for completion and reversal

### `idempotency_records`

- idempotency key
- operation type
- request hash
- processing/completed state
- stored response metadata

### `reversal_transactions`

- original transfer linkage
- reversal status and timestamps

### `outbox_events`

- event metadata
- target Kafka topic
- payload JSON
- retry status
- next retry time

### `dead_letter_events`

- permanently failed outbox events after retry exhaustion

Notable constraints:

- one wallet per `customer_id + currency`
- non-negative wallet balance
- positive ledger and transfer amounts
- transfer source and destination must differ
- one reversal per original transfer

## Messaging and Outbox

The service uses the transactional outbox pattern to avoid writing business state and publishing Kafka messages in separate, inconsistent steps.

Topics:

- `fintech.transfer.completed`
- `fintech.transfer.reversed`

Produced event payloads:

### Transfer completed event

- `transferId`
- `sourceWalletId`
- `destinationWalletId`
- `amount`
- `currency`
- `reason`
- `occurredAt`

### Transfer reversed event

- `reversalId`
- `originalTransferId`
- `sourceWalletId`
- `destinationWalletId`
- `amount`
- `currency`
- `reason`
- `occurredAt`

Retry behavior:

- scheduler polls every `5 seconds`
- initial retry delay is `2 seconds`
- exponential backoff multiplier is `2.0`
- max delay is `60 seconds`
- max attempts is `5`
- final failure moves the event to `dead_letter_events`

Kafka UI is available locally at:

```text
http://localhost:8085
```

## Concurrency Notes

Wallet entities use optimistic locking through `@Version`.

If concurrent modification happens, the API returns a conflict error:

- code: `CONCURRENT_MODIFICATION`
- status: `409 Conflict`

This matters most during balance-changing operations such as debit, transfer, and reversal.

## Testing

The repository includes:

- unit tests for services, mappers, and utilities
- integration tests for wallet, transfer, reversal, outbox, and exception handling
- concurrency and idempotency-focused transfer tests

Testcontainers-based integration coverage exists for:

- PostgreSQL
- Kafka

Coverage reporting is configured through JaCoCo during Maven packaging.

## Using the Provided Postman Collection

A Postman collection was provided at:

```text
c:\Users\mohan\OneDrive\Desktop\Wallet App.postman_collection.json
```

The collection covers:

- wallet creation
- wallet retrieval
- wallet credit
- wallet debit
- wallet ledger retrieval
- transfer creation
- duplicate transfer/idempotency scenario
- transfer reversal

How to use it:

1. Start PostgreSQL and Kafka with `docker compose up -d`.
2. Start the Spring Boot application on `localhost:8080`.
3. Import the Postman collection into Postman.
4. Replace placeholder IDs such as `{walletId}` and `{transferId}` with real values returned by the API.
5. For transfer testing, keep the `Idempotency-Key` header when validating duplicate request behavior.

Recommended test sequence:

1. Create wallet A
2. Create wallet B
3. Credit wallet A
4. Transfer from wallet A to wallet B
5. Repeat the same transfer request with the same `Idempotency-Key`
6. Inspect both wallet ledgers
7. Reverse the transfer

## Operational Notes

- Schema management is handled by Flyway on application startup.
- The app expects PostgreSQL to be reachable before startup.
- Kafka must be available if you want outbox publication to succeed.
- If Kafka is down, business transactions can still persist, but outbox events will retry and may eventually be dead-lettered.
- Logging includes correlation ID propagation for request tracing.

## Current Gaps and Recommended Next Steps

### Highest-priority gap

Add authentication and authorization before any real deployment.

Recommended target design:

- OAuth 2.0 / OpenID Connect
- PKCE for public clients
- JWT bearer token validation in Spring Security
- role/scope-based access control
- customer-to-wallet ownership enforcement
- admin-only reversal permissions
- audit trail for user identity and privileged actions

### Additional improvements

- OpenAPI/Swagger documentation
- API versioning strategy
- rate limiting
- audit logging for business actions
- metrics and alerts for failed outbox publications
- environment-specific configuration profiles
- containerized app runtime for one-command startup

## Summary

This project already demonstrates several important backend engineering patterns:

- transactional wallet bookkeeping
- ledger-based money movement tracking
- transfer idempotency
- optimistic locking
- transactional outbox with retries and dead-lettering
- integration testing with Testcontainers

The main blocker for calling it complete as a fintech-grade service is the missing security layer. That should be treated as the next critical implementation step.
