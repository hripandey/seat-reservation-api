# 1. Requirements

## 1.1 Functional Requirements

### FR-1: Create a Show

- Admin can create a show using `POST /show`.
- A show contains:
    - Show name
    - List of unique seat numbers
    - Ticket price in integer minor units (paise)
- All seats are created in `AVAILABLE` state.
- Each show has a unique identifier.

### FR-2: Reserve Seats

- An authenticated user can reserve one or more seats using `POST /show/{showId}/reserve`.
- User identity must be derived from the authentication token and must not be accepted from the request body.
- A successful reservation returns a unique reservation ID and the total amount in paise.
- A seat can belong to at most one active reservation.

### FR-3: Prevent Double Booking

- Concurrent requests for the same seat must be resolved atomically.
- Exactly one request can successfully reserve a seat.
- Losing requests receive `409 Conflict` rather than a server error.
- The database is the source of truth for seat ownership.

### FR-4: Enforce Per-User Limits

- A user can reserve at most `per_user_limit` seats for a show.
- The default limit is 4 seats.
- The limit must remain enforced under concurrent requests from the same user.

### FR-5: Idempotent Reservations

- Every reservation request must provide an idempotency key.
- Retrying a request with the same key and identical request data returns the original reservation.
- Reusing an idempotency key with different request data returns `409 Conflict`.
- A retry must not create an additional reservation or change seat ownership.

### FR-6: Multi-Seat Reservation

- Multi-seat reservations use an **all-or-nothing** model.
- If all requested seats are available, all seats are reserved atomically.
- If any requested seat cannot be reserved, none of the requested seats are allocated.

### FR-7: Cancel / Release Reservation

- A reservation owner can cancel their reservation.
- Another user cannot cancel someone else's reservation.
- Cancelling an active hold makes the associated seats available again.
- A cancellation must not overwrite a newer reservation.

### FR-8: Hold Expiration

- Holds are time-bound and automatically expire after the configured hold duration.
- Expired seats become available for reservation.
- Expiration must be concurrency-safe and must not release a seat that has already been re-reserved.

### FR-9: View Show State

- `GET /show/{showId}` returns:
    - Show information
    - Per-seat status
    - Total seats
    - Available seats
    - Held seats
    - Confirmed seats
- The reconciliation invariant must always hold:

```text
available + held + confirmed = total
```

### FR-10: Authentication and Authorization

- Reservation operations require authentication.
- Users can only cancel their own reservations.
- Identity is derived exclusively from the authentication token.

### FR-11: Health and Metrics

- The service exposes health information.
- The service records relevant reservation and infrastructure metrics.
- Domain-level contention and validation failures must return appropriate `4xx` responses rather than `5xx` errors.

## 1.2 Non-Functional Requirements

### NFR-1: Correctness

The system must never assign the same seat to two users.

For a concurrent race on a single seat:

```text
500 concurrent requests
        ↓
1 successful reservation
499 rejected with 409
```

### NFR-2: Concurrency

The system must support approximately 20,000 concurrent reservation requests against a fresh show, including heavy
contention on a small number of seats.

### NFR-3: Atomicity

Seat allocation must be performed as an atomic database operation/transaction.

A read-then-write sequence that can observe stale seat state must not be used as the correctness mechanism.

### NFR-4: Consistency

The system must maintain the seat-state invariant:

```text
available + held + confirmed = total
```

This invariant must hold during and after concurrent reservation activity.

### NFR-5: Idempotency

Network retries must not result in duplicate reservations or additional seat allocations.

### NFR-6: Availability

Normal business conflicts, such as an already-reserved seat or an exceeded seat limit, must not cause server errors.

The system should return deterministic `4xx` responses for expected domain failures.

### NFR-7: Performance

The reservation endpoint should provide predictable latency under normal load and remain responsive during
high-contention scenarios.

Target latency should be defined based on the deployment environment.

### NFR-8: Scalability

The API layer should be stateless and capable of running multiple instances.

Seat ownership and concurrency correctness must not depend on in-memory state within a single application instance.

### NFR-9: Durability

Confirmed reservations and seat ownership must survive application restarts.

The database should be treated as the persistent source of truth.

### NFR-10: Observability

The system should expose:

- Request count
- Reservation success count
- Reservation conflict count
- Error count
- Reservation latency
- Database connection pool metrics
- Active holds
- Confirmed reservations

### NFR-11: Security

- Authentication is required for user operations.
- Authorization must be enforced for reservation ownership.
- User identity must not be trusted from client-provided fields.
- Sensitive authentication information must not be logged.

### NFR-12: Monetary Accuracy

All monetary values must be represented as integer minor units (paise).

Floating-point types must not be used for money.

---

## 1.3 Key Design Decisions

The following decisions are made to remove ambiguity from the requirements:

| Decision             | Choice                              |
|----------------------|-------------------------------------|
| Multi-seat booking   | All-or-nothing                      |
| Seat contention      | Database-enforced atomic allocation |
| Identity             | Authentication token                |
| User seat limit      | 4 by default                        |
| Idempotency          | Required                            |
| Money representation | Integer paise                       |
| Persistence          | PostgreSQL                          |
| Seat state           | `AVAILABLE`, `HELD`, `CONFIRMED`    |
| Expected contention  | `409 Conflict`                      |
| Database             | Source of truth                     |
| API architecture     | Stateless Spring Boot services      |

# DB Model

```
seat_reservation
│
├── shows
│   ├── id
│   ├── name
│   └── price_paise
│
└── seats
    ├── id
    ├── show_id      ← FK to shows.id
    ├── seat_number
    └── status
```