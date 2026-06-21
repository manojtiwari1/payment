# API Documentation

Base URL: `http://localhost:8080`

## Response envelope

Every endpoint returns a common envelope:

```json
{
  "status": 200,
  "date": "2026-06-20T16:48:47Z",
  "data": { },
  "code": "ENTITY",
  "errors": null,
  "path": "/payments"
}
```

On error, `data` is null and `errors` carries messages:

```json
{
  "status": 409,
  "code": "IDEMPOTENCY_CONFLICT",
  "errors": ["Idempotency-Key already used with a different request payload."],
  "path": "/payments"
}
```

### Status codes

| HTTP | When |
|------|------|
| 200 | Success |
| 400 | Validation / bad request (`BAD_REQUEST`, `INVALID_STATE_TRANSITION`, `INVALID_PARAMETER`) |
| 401 | Missing/invalid/expired/blacklisted token |
| 403 | Authenticated but not allowed (`ACCESS_DENIED`) |
| 404 | Not found (`NOT_FOUND`) |
| 409 | Conflict (`IDEMPOTENCY_CONFLICT`, `CONFLICT`) |
| 429 | Rate limit exceeded (`TOO_MANY_REQUESTS`) — per-merchant, 100 req/min |
| 500 | Unhandled error |

> **Correlation id:** every response carries an `X-Correlation-Id` header (echoed from the request or
> generated). Send your own to correlate client/server logs.

## Authentication

Self-contained JWT. Obtain a token via login, then send `Authorization: Bearer <token>` on
protected endpoints. Access tokens last 1h; refresh tokens 7d (refresh rotates the pair).

JWT claims: `sub` (user id), `email`, `username`, `merchantCode`, `authorities`, `type`
(`ACCESS`/`REFRESH`), `jti`.

---

### POST /api/auth/login  · public

```json
{ "userName": "merchant@payment.local", "password": "Merchant@12345" }
```

`data` (JwtResponse): `token`, `refreshToken`, `id`, `email`, `userName`, `firstName`, `lastName`,
`fullName`, `contactNo`, `expireIn`, `refreshExpireIn`. Invalid credentials → 400.

### POST /api/auth/refresh  · public

```json
{ "refreshToken": "<refresh token>" }
```
Returns a new token pair; the presented refresh token is revoked (rotation). Reusing it → 403.
Passing an **access** token here → 403 (type check).

### POST /api/auth/logout  · authenticated

Header `Authorization: Bearer <access token>`; optional body `{ "refreshToken": "..." }`. Blacklists
the token jti(s). Subsequent use of the access token → 401.

---

## Users

### POST /api/auth/users  · public (self-registration)

Creates a user and returns a **JWT token pair** so the account is logged in immediately.

```json
{
  "roleId": 2,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "mobileNo": "9876543210",
  "password": "John@12345",
  "confirmPassword": "John@12345",
  "merchantCode": "M123"
}
```

`data` is a JwtResponse (same shape as login). Duplicate email/phone → 400 (`DUPLICATE`). Password
must satisfy: ≥8 chars with upper, lower, digit, and special character.

> `merchantCode` is optional; set it for MERCHANT users. `roleId` must be an existing active role.

---

## Payments

### POST /payments  · MERCHANT or ADMIN

Headers: `Authorization: Bearer <token>`, `Idempotency-Key: <key>` (**required**).

```json
{ "amount": 100.00, "currency": "EUR", "customerId": "C456" }
```

- MERCHANT: the merchant is taken from the token (`merchantCode`); a `merchantId` in the body is ignored.
- ADMIN: must include `"merchantId": "M123"` in the body.

Response — `data` (PaymentResponse):

```json
{
  "paymentId": "PAY-ab12cd34ef01",
  "status": "PENDING",
  "merchantId": "M123",
  "customerId": "C456",
  "amount": 100.00,
  "currency": "EUR",
  "selectedPsp": null,
  "createdAt": "2026-06-20T16:48:47Z"
}
```

Routing runs asynchronously after the response. Behavior:

| Case | Result |
|------|--------|
| Same `Idempotency-Key`, same body | Returns the **same** payment (no duplicate), 200 |
| Same key, **different** body | 409 `IDEMPOTENCY_CONFLICT` |
| Missing `Idempotency-Key` | 400 |
| `amount` ≤ 0 or bad `currency` | 400 (validation) |
| More than 100 creates/min for the merchant | 429 `TOO_MANY_REQUESTS` |

### GET /payments/{reference}  · MERCHANT or ADMIN

Returns one payment. A MERCHANT may only read its own merchant's payments (else 403); ADMIN reads any.
Unknown reference → 404.

### GET /payments?page=0&size=20  · MERCHANT or ADMIN

Paged list. MERCHANT sees only its own payments; ADMIN sees all. `data` is a page envelope
(`content`, `totalElements`, `totalPages`, `size`, `number`, `numberOfElements`).

---

## Webhooks

### POST /webhooks/{psp}  · public

`{psp}` ∈ `PSP_A` | `PSP_B` | `PSP_C`.

```json
{ "eventId": "EV123", "paymentId": "PAY-ab12cd34ef01", "status": "SUCCESS" }
```

`data`: `{ "eventId": "EV123", "result": "PROCESSED" }`.

| `result` | Meaning |
|----------|---------|
| `PROCESSED` | First delivery; applied (or acknowledged) |
| `DUPLICATE` | Same `eventId` seen before — ignored |
| `UNKNOWN_PAYMENT` | `paymentId` not found — recorded, not applied |

The state change applies only if the [state machine](DATABASE.md) permits it from the current status
(out-of-order/late events are dropped). Unknown `psp` or `status` → 400.

---

## Operations (ADMIN)

### POST /admin/reconciliation/run  · ADMIN

Triggers a reconciliation pass on demand. `data`: `{ "corrected": <n> }`.

### GET /actuator/health · /actuator/info · /actuator/metrics

Spring Boot Actuator endpoints (exposed per `management.endpoints.web.exposure.include`). Domain
counters: `/actuator/metrics/payment_success_total`, `/payment_failure_total`,
`/webhook_processed_total`, `/reconciliation_runs_total`.

---

## Quick reference

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/login` | public |
| POST | `/api/auth/refresh` | public |
| POST | `/api/auth/logout` | authenticated |
| POST | `/api/auth/users` | public |
| POST | `/payments` | MERCHANT/ADMIN |
| GET | `/payments/{reference}` | MERCHANT/ADMIN |
| GET | `/payments` | MERCHANT/ADMIN |
| POST | `/webhooks/{psp}` | public |
| POST | `/admin/reconciliation/run` | ADMIN |
