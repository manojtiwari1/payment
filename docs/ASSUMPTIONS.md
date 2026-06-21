# Assumptions & Trade-offs

This documents the decisions taken while building the platform, why they were made, and their known
limitations. Grouped by theme.

## Platform & stack

- **Extends an existing Spring Boot 4 / Java 25 / Gradle project** rather than the spec's Maven /
  Java 21 / Spring Boot 3. Rationale: a working JWT/auth, response-envelope, exception-handling, and
  JPA-auditing foundation already existed and was reused. Trade-off: versions differ from the brief,
  and some spec libraries (Resilience4j Spring Boot starter) don't cleanly autoconfigure on Boot 4 —
  addressed by using Resilience4j's **programmatic core API** (planned Phase 4).
- **PSPs are simulated** (`SimulatedPspGateway`) with per-PSP configurable outcomes
  (`SUCCESS`/`TRANSIENT_FAILURE`/`TIMEOUT`/`SLOW`). No real network calls.
- **PostgreSQL is the source of truth.** Redis is used only for JWT revocation (logout/refresh
  rotation). Eventual consistency is acceptable for downstream consumers.

## Payment creation: async routing

- **`POST /payments` returns `PENDING` and routes after commit.** The spec's response contract
  (`status: PENDING`) is honored over its "synchronous execution" note. The payment is persisted and
  committed first; routing then runs on a background thread via a
  `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` handler.
- **Consequence:** the final status (`SUCCESS`/`FAILED`/still `PROCESSING`) is observed via
  `GET /payments/{ref}` or a webhook, not in the create response.
- **No DB transaction is held across a PSP call.** `RoutingEngine` orchestrates; each DB mutation is
  a separate short transaction in `PaymentProcessingService`.
- **Async executor:** `@EnableAsync` currently uses the default `SimpleAsyncTaskExecutor`
  (thread-per-task). A bounded pool is a Phase-4 hardening item.
- **Auditing of async work:** the routing thread has no `SecurityContext`, so routing-driven
  `payments`/`payment_attempts` audit columns are stamped `SYSTEM` rather than the merchant. Intended.

## Idempotency (correctness centerpiece)

- **DB-enforced, not in-memory.** `UNIQUE(merchant_code, idempotency_key)` plus
  `INSERT ... ON CONFLICT DO NOTHING`. The payment row and the idempotency row are written in **one
  transaction**; a losing concurrent request throws an internal marker, rolling back its own payment,
  then re-reads and returns the winner's payment. Correct across multiple instances.
- **Same key + different payload → 409** via a SHA-256 `request_hash` comparison.
- **`Idempotency-Key` is required** for `POST /payments` (missing → 400).
- Trade-off: relies on PostgreSQL `ON CONFLICT` semantics (a native query); not portable to engines
  without upsert.

## Routing, failover & the timeout decision

- **Failover** walks the merchant's configured PSP list left-to-right; first `SUCCESS` wins, all
  `FAILED` → payment `FAILED`. Every attempt is recorded in `payment_attempts`.
- **A PSP timeout is treated as `INDETERMINATE`, not a failover trigger.** Rationale: a timeout may
  mean the charge *did* succeed, so failing over to the next PSP risks a **double charge**. The
  payment is left in `PROCESSING` for reconciliation/webhook to resolve. This deviates from a naive
  "retry on timeout" but is the safer real-world behavior.

## State machine

- Strict allowed-transition table: `CREATED→PENDING→PROCESSING→SUCCESS/FAILED→REFUNDED`. Illegal
  moves throw `INVALID_STATE_TRANSITION` (400). This is what makes webhooks **out-of-order safe**.
- **Consequence:** a `SUCCESS` webhook that arrives while the payment is still `PENDING` (routing
  hasn't flipped it to `PROCESSING`) is dropped, because `PENDING→SUCCESS` isn't allowed. The system
  converges via routing or reconciliation. A direct consequence of async routing + a strict table.

## Webhooks

- **Idempotent** via `UNIQUE(event_id)` — duplicate deliveries return `DUPLICATE`.
- **Race-safe** via a transaction-scoped `pg_advisory_xact_lock` keyed by the payment reference,
  acquired **before** reading the payment so serialized deliveries see fresh state.
- **Webhook-vs-routing race:** routing does **not** take the advisory lock, so a webhook landing
  exactly as routing concludes can collide on `@Version` and one side gets an optimistic-lock error
  (surfaced as 500). Accepted; `@Version` is the intended backstop and the window is small.
- Webhooks are **public** (PSPs have no JWT). A real deployment would verify a PSP signature/HMAC.

## Persistence & locking

- **`@Version` optimistic locking** on `payments` prevents lost updates.
- **Transaction-scoped advisory locks** (`pg_advisory_xact_lock` / `pg_try_advisory_xact_lock`) —
  never session-scoped — so locks auto-release at commit/rollback and never leak across the pool.
- **`payment_id` references are logical** (plain `bigint`), not DB foreign keys — chosen for
  decoupling between modules and write throughput.
- **Schema via `ddl-auto=update`**; **Flyway is deferred to Phase 5.** Trade-off recorded so
  "deferred" doesn't become "never": production should switch to `validate` + versioned migrations.

## Security

- **Merchant scoping is derived from the JWT** (`merchantCode` claim), never trusted from the request
  body — a MERCHANT cannot create or read another merchant's payments.
- **Lightweight merchant model:** `merchantCode` is a string column on `users` and a JWT claim; there
  is no separate `Merchant` entity. Routing config is keyed by merchant code. A full `Merchant`
  aggregate can be added later if needed.
- **`/api/auth/users/**` is currently public** (to allow self-registration). ⚠️ This also exposes
  `GET /api/auth/users` (list users) publicly — **should be restricted to ADMIN** before any real
  deployment.
- **Secrets in `application-dev.properties`** (JWT signing key, DB/Redis passwords) are for local dev
  only — externalize via environment/secret manager in production.
- **Bootstrap seeder** creates default admin/merchant accounts with known passwords when the DB is
  empty — change immediately outside local dev; disable with `app.bootstrap.admin.enabled=false`.

## Build & verification

- The code was authored in an environment where **Gradle could not start** (a JVM loopback-binding
  failure unrelated to the code). It is reviewed and statically verified (imports, dependency
  bytecode, API signatures), but **`./gradlew build` on a normal machine is the real gate.**
- Runtime-only behaviors — advisory locks, the scheduler, webhook idempotency under concurrency —
  can only be validated against a real PostgreSQL. The **100-concurrent-requests / one
  idempotency-key → exactly one payment** Testcontainers test is a Phase-5 deliverable.
- **Demonstrating reconciliation:** create a payment as ADMIN for `merchantId: "M_STUCK"` (routes to
  `PSP_C`, configured to time out → left `PROCESSING`), set
  `payment.reconciliation.stuck-threshold-minutes=0`, then `POST /admin/reconciliation/run`.

## Events & outbox (Phase 3)

- **Transactional outbox:** every payment state change writes an `outbox_events` row in the **same
  transaction** (`OutboxWriter` is `Propagation.MANDATORY`, so it fails fast if ever called outside a
  transaction). A scheduled `OutboxRelay` ships unpublished rows to Kafka and marks them published.
- **At-least-once delivery:** the relay marks a row published only after a broker ack; if it crashes
  between ack and mark, the event re-sends. Consumers must dedupe on `eventId` (carried in payload and
  as a unique column).
- **Broker-down tolerant:** the app starts without Kafka; the relay logs and retries on its next tick,
  stopping the batch at the first failure to preserve ordering and back off.
- **Events:** `PAYMENT_CREATED`, `PAYMENT_PROCESSING`, `PAYMENT_SUCCEEDED`, `PAYMENT_FAILED`
  (no `REFUNDED` event yet — no refund flow). Payload: `{eventId, paymentId, merchantId, status, timestamp}`.
- **Ordering:** the relay keys each Kafka send by the payment reference and sends in `id` order, so a
  given payment's events land on one partition in order (CREATED → PROCESSING → SUCCEEDED). Don't
  switch the relay to parallel sends or a null key.
- Payload JSON is built by hand to stay independent of the active Jackson version (the module mixes
  Jackson 2 and 3 mappers).
- **Trade-off (deliberate):** `OutboxRelay.publishPending()` wraps the whole batch in one transaction,
  so a DB connection is held across the synchronous Kafka acks. Fine at assignment scale; under load,
  refactor to mark-published per event (or send outside the tx, mark inside) to avoid pool starvation.

## Resilience & observability (Phase 4)

- **Resilience4j is used programmatically** (registries + low-level `CircuitBreaker`/`RateLimiter`
  API), not via the Spring Boot starter — deliberately, to avoid the Boot-4 autoconfig gap noted above.
- **Circuit breaker per PSP** (keyed by PSP name) inside the routing loop: opens at a 50% failure rate
  over a 10-call window, stays open 30s. Business failures **and timeouts** count against it (via the
  low-level `onError`/`onSuccess` after `tryAcquirePermission`), not just thrown exceptions. An open
  breaker is skipped and routing fails over — so a sick PSP is bypassed without per-call latency.
- **Rate limiter per merchant** (100 req/min, no waiting) at payment creation → `429 TOO_MANY_REQUESTS`
  when exceeded. Note: rate-limit counts every create call, including idempotent replays.
- **Correlation id**: `CorrelationIdFilter` (highest precedence) reads/generates `X-Correlation-Id`,
  puts it in the SLF4J MDC (surfaced in every log line via `logging.pattern.level`) and echoes it on
  the response. Per-request only — `paymentId`/`merchantId` are logged in domain messages, not forced
  into the MDC globally.
- **Metrics** (Micrometer, via Actuator at `/actuator/metrics/<name>`): `payment_success_total`,
  `payment_failure_total`, `webhook_processed_total` (tagged `result`), `reconciliation_runs_total`.
  Counted at the single transition point for each terminal state, so the state machine's
  no-re-transition guarantee means no double-counting across routing/webhook/reconciliation sources.
- Registries/counters are created lazily per key (PSP / merchant), so cardinality tracks real traffic.

## Not yet implemented (roadmap)

| Capability | Phase |
|------------|-------|
| **Flyway** migrations | 5 |
| **Testcontainers** integration & concurrency tests | 5 |
