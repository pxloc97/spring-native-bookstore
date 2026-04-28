# Order-Service ↔ Inventory-Service — Integration Plan

## Goal

Make inventory reservation a first-class step in order processing, with deterministic behavior locally and in CI:

- `order-service` publishes order lifecycle events (`order.created`, `order.cancelled`) to Kafka.
- `inventory-service` consumes those events, reserves/releases stock, and publishes decisions (`inventory.reserved`, `inventory.rejected`).
- `order-service` consumes inventory decisions and transitions orders accordingly.

This plan assumes Spring Cloud Stream function model (same style already used in both services).

---

## Event Contract

### Topic: `order-events`

- Key: `orderId`
- Events:
  - `order.created`: `{ orderId, items: [{ isbn, quantity }] }`
  - `order.cancelled`: `{ orderId }`

### Topic: `inventory-events`

- Key: `orderId`
- Events:
  - `inventory.reserved`: `{ orderId, status: "RESERVED", reason: null }`
  - `inventory.rejected`: `{ orderId, status: "REJECTED", reason }`

Recommendation: include an explicit `type` header (or payload field) so a single topic can carry multiple event types safely.

---

## Integration Steps

### Step 1 — Publish `order.created` / `order.cancelled` (order-service)

- Add an outbound port + adapter to publish to `order-events`.
- Publish `order.created` after a request is accepted (or after persistence, depending on domain rules).
- Publish `order.cancelled` when cancellation API/flow exists.

**Tests**
- `KafkaOrderEventsPublisherTest`: uses Stream test binder `OutputDestination` to assert payload and headers.

### Step 2 — Consume order events and publish decisions (inventory-service)

- Ensure `reserveStock` consumes `order.created`.
- Ensure `releaseStock` consumes `order.cancelled`.
- Publish `inventory-events` decisions via `StreamBridge`.
- Keep duplicate `order.created` idempotent (DB unique constraint `(order_id, isbn)`).

**Tests**
- `OrderEventConsumerTest`: Stream test binder + Postgres Testcontainers.
  - created → RESERVED decision and stock decremented once
  - duplicate created → no double decrement

### Step 3 — Consume inventory decisions (order-service)

- Add a consumer for `inventory-events`.
- When `RESERVED`: mark order as ACCEPTED and publish existing `order-accepted` (so dispatcher flow remains unchanged).
- When `REJECTED`: mark order as REJECTED and do not publish `order-accepted`.

**Tests**
- `InventoryDecisionConsumerTest`: Stream test binder `InputDestination` to send a decision and assert order state transitions.

### Step 4 — Wiring (Config Server + Docker + Kubernetes)

- Add `config/inventory-service*.yml` and update bindings/profiles.
- Ensure Docker Compose and k8s manifests set `SPRING_PROFILES_ACTIVE=prod` so config server `*-prod.yml` takes effect.

**Tests**
- Keep unit tests free of Kafka/Postgres startup.
- Keep integration tests behind Testcontainers and enabled in CI where Docker is available.

