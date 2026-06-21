# Payment Orchestration Platform

A Spring Boot service that accepts merchant payment requests, routes them to Payment Service
Providers (PSPs) with retry/failover, enforces **idempotency** and a **payment state machine**,
processes **asynchronous webhooks**, and **reconciles** stuck payments — all on PostgreSQL as the
source of truth, secured with self-contained JWT auth.

> **Status:** Phases 1–2 are implemented (core payment flow, idempotency, PSP routing/failover,
> state machine, webhooks, reconciliation, JWT auth with merchant scoping). Phases 3–5
> (Kafka/outbox events, circuit breaker + rate limiting + correlation IDs/metrics, Flyway +
> Testcontainers) are planned — see [Roadmap](#roadmap).

## Documentation

| Doc | Contents |
|-----|----------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Component & flow diagrams, request lifecycle |
| [docs/DATABASE.md](docs/DATABASE.md) | Schema: tables, columns, constraints, indexes |
| [docs/API.md](docs/API.md) | Endpoints, request/response examples, status codes |
| [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md) | Assumptions, trade-offs, deviations from the spec |

## Tech stack

| | |
|---|---|
| Language / build | **Java 25** (Gradle toolchain), Gradle wrapper |
| Framework | **Spring Boot 4.1**, Spring Security, Spring Data JPA |
| Database | **PostgreSQL** (source of truth; `@Version` optimistic locking, advisory locks) |
| Cache / revocation | **Redis** (JWT blacklist on logout) |
| Auth | Self-contained **JWT** (jjwt 0.13) — access + refresh tokens |
| Events | **Kafka** + transactional **outbox** relay |
| Resilience | **Resilience4j** (programmatic) — circuit breaker per PSP, rate limiter per merchant |
| Observability | `X-Correlation-Id` in logs, **Micrometer** counters via Actuator |

> The original brief named Maven / Java 21 / Spring Boot 3 / Kafka. This implementation **extends an
> existing Spring Boot 4 / Java 25 / Gradle codebase** (reusing its JWT auth, response/exception
> framework, and auditing) rather than starting fresh. See [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md).

## Prerequisites

- **JDK 25** (the Gradle toolchain will use it; install a JDK 25 or let Gradle provision one)
- **PostgreSQL 14+** running on `localhost:5432`
- **Redis** running on `localhost:6379`
- **Kafka** running on `localhost:9092` (for domain-event publishing)

The app still **starts without Kafka** — outbox events simply accumulate unpublished and ship once a
broker is reachable. The quickest way to get all three is `docker compose up -d` (see below).

## Setup

**Fastest path — everything via Docker:**

```bash
docker compose up -d   # starts PostgreSQL, Redis, and Kafka (see docker-compose.yml)
```

This brings up Postgres (db `payment`, user `postgres`/`root`), Redis (password `Worked@2025`), and a
single-node Kafka on `localhost:9092`. If your `application-dev.properties` uses different values,
either align them or edit the compose file. Otherwise, provision the three services manually:

1. **Create the database** (defaults from `application-dev.properties`):

   ```sql
   CREATE DATABASE payment_platform;
   ```
   Default credentials: user `postgres`, password `root`. Adjust in
   `src/main/resources/application-dev.properties` if yours differ.

2. **Redis** must be reachable at `localhost:6379` with password `Payment@2025` (or update the
   `spring.data.redis.*` properties).

3. **Schema** is created automatically — `spring.jpa.hibernate.ddl-auto=update` builds all tables on
   first start. (Flyway migrations are a Phase-5 item.)

## Run

```bash
./gradlew bootRun
```

The `dev` profile is active by default (`spring.profiles.active=dev`). The app listens on
**http://localhost:8080**.

On first start against an empty database, a bootstrap seeder creates two accounts:

| Role | Email | Password | merchantCode |
|------|-------|----------|--------------|
| ADMIN | `admin@payment.local` | `Admin@12345` | – |
| MERCHANT | `merchant@payment.local` | `Merchant@12345` | `M123` |

> Change these immediately outside local dev. Seeding only runs when the `users` table is empty and
> can be disabled with `app.bootstrap.admin.enabled=false`.

## Quick start (happy path)

```bash
# 1. Log in as the merchant → copy data.token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userName":"merchant@payment.local","password":"Merchant@12345"}'

# 2. Create a payment (idempotent)
curl -s -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Idempotency-Key: abc123' \
  -d '{"amount":100.00,"currency":"EUR","customerId":"C456"}'
# → { "data": { "paymentId":"PAY-...", "status":"PENDING", ... } }

# 3. Read it back — routing has run (PSP_A fails → PSP_B succeeds)
curl -s http://localhost:8080/payments/PAY-... -H 'Authorization: Bearer <TOKEN>'
# → status SUCCESS, selectedPsp PSP_B
```

Full request/response details: [docs/API.md](docs/API.md).

## Build & test

```bash
./gradlew build      # compile + tests
./gradlew test       # tests only
```

> **Build environment note:** these docs were produced in a sandbox where Gradle could not start
> (a JVM loopback-binding failure). All code is reviewed and statically verified, but **`./gradlew
> build` on your machine is the real gate.** A Testcontainers-backed concurrency test (100 concurrent
> requests with one idempotency key → exactly one payment) is a Phase-5 deliverable.

## Configuration reference

All settings live in `src/main/resources/application-dev.properties`. Highlights:

| Property | Default | Purpose |
|----------|---------|---------|
| `security.jwt.expiration` | `3600000` (1h) | Access-token lifetime (ms) |
| `security.jwt.refresh-token.expiration` | `604800000` (7d) | Refresh-token lifetime (ms) |
| `payment.routing.merchants.<code>` | – | Ordered PSP failover list per merchant |
| `payment.routing.default-providers` | `PSP_A,PSP_B` | Fallback PSP order |
| `payment.psp.providers.<PSP>.outcome` | `SUCCESS` | Simulated outcome: `SUCCESS`/`TRANSIENT_FAILURE`/`TIMEOUT`/`SLOW` |
| `payment.reconciliation.cron` | `0 0 * * * *` | Reconciliation schedule (hourly) |
| `payment.reconciliation.stuck-threshold-minutes` | `120` | Age before a PROCESSING payment is reconciled |

## Roadmap

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Payments, idempotency, PSP routing/failover, state machine, JWT + merchant scoping | ✅ Done |
| 2 | Webhooks (idempotent, out-of-order safe), reconciliation, advisory locks | ✅ Done |
| 3 | Domain events via transactional **outbox** → **Kafka** | ✅ Done |
| 4 | **Resilience4j** circuit breaker + rate limiting, `X-Correlation-Id`, Micrometer metrics | ✅ Done |
| 5 | **Flyway** migrations, **Testcontainers** integration + concurrency tests, docker-compose | ✅ Done |
