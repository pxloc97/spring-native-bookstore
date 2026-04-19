# Inventory Service — Implementation Plan (Hexagonal Architecture)

## Goals

Build `inventory-service` as the single Source of Truth for stock in the Bookstore system. Epic 1 covers three core capabilities:

- **Capability 1 (Admin/Ops):** Manage and query stock — `addStock`, `reduceStock`, `queryStock`.
- **Capability 2 (Core Business):** Reserve stock for an order using an *All-or-Nothing* policy.
- **Capability 3 (Reversal):** Release stock safely and idempotently by `orderId`.

Stack mirrors `order-service`: **Spring WebFlux + R2DBC + Kafka + Flyway**, reactive end-to-end.

---

## Decisions

- Architecture style: Hexagonal (Ports & Adapters), same package structure as `order-service`.
- Reactive model: WebFlux + R2DBC — keeps parity with reference architecture; appropriate for high-throughput, contention-prone inventory.
- Persistence: R2DBC + PostgreSQL + **jOOQ** — `DSLContext` used in persistence adapters (same as `order-service`); no Spring Data repositories in the production path.
- Concurrency: Optimistic Locking via `version` column on `inventory` table.
- Idempotency: unique constraint `(order_id, isbn)` on `reservation` table.
- Messaging: Kafka — consume `order-events`, publish `inventory-events`.
- Schema: Flyway migrations, incremental versioned scripts.
- Formatting: Spotless plugin, same config as `order-service`.

---

## Domain Model

Domain layer is **pure Java** — zero Spring, R2DBC, or Kafka imports.

### `InventoryItem`

| Field | Type | Notes |
|---|---|---|
| `isbn` | `String` | natural key |
| `availableQuantity` | `int` | stock on hand |
| `reservedQuantity` | `int` | stock held for pending orders |

**Business methods:**

- `reserve(int quantity)` — decrements `availableQuantity`, increments `reservedQuantity`. Throws `InsufficientStockException` if `availableQuantity < quantity`.
- `release(int quantity)` — increments `availableQuantity`, decrements `reservedQuantity`. Throws `DomainException` if `reservedQuantity < quantity`.
- `adjust(int delta)` — adds delta to `availableQuantity`. Throws `DomainException` if result < 0.

### `Reservation`

| Field | Type | Notes |
|---|---|---|
| `reservationId` | `UUID` | surrogate key |
| `orderId` | `UUID` | FK reference to order |
| `isbn` | `String` | |
| `quantity` | `int` | |
| `status` | `ReservationStatus` | `RESERVED`, `RELEASED`, `REJECTED` |

**Invariant:** one reservation record per `(orderId, isbn)` pair — enforced at DB level.

### `InventoryDecision` (event data)

Carries the outcome published back to `order-service`:
- `RESERVED` — all line items reserved successfully.
- `REJECTED` — at least one line item had insufficient stock (includes `reason`).

### Exceptions

- `InsufficientStockException extends InventoryException` — raised by `reserve()`.
- `InventoryException` — base domain exception.

---

## Package Structure

```
com.locpham.bookstore.inventoryservice
├── domain
│   ├── InventoryItem.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── InventoryDecision.java
│   ├── InventoryException.java
│   └── InsufficientStockException.java
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── ManageStockUseCase.java
│   │   │   ├── ReserveStockUseCase.java
│   │   │   └── ReleaseStockUseCase.java
│   │   └── out
│   │       ├── InventoryPort.java
│   │       ├── ReservationPort.java
│   │       └── InventoryEventPublisher.java
│   └── service
│       ├── StockManagementService.java
│       ├── ReserveStockService.java
│       └── ReleaseStockService.java
├── adapter
│   ├── in
│   │   ├── web
│   │   │   └── InventoryController.java
│   │   └── messaging
│   │       └── OrderEventConsumer.java
│   └── out
│       ├── persistence
│       │   ├── jooq
│       │   │   ├── generated/                         ← code-gen output (gitignored)
│       │   │   ├── JooqInventoryRepositoryImpl.java   ← implements InventoryPort
│       │   │   ├── JooqReservationRepositoryImpl.java ← implements ReservationPort
│       │   │   ├── JooqInventoryMapper.java
│       │   │   └── JooqReservationMapper.java
│       └── messaging
│           └── KafkaInventoryEventPublisher.java
└── bootstrap
    ├── InventoryServiceApplication.java
    └── config
        └── JooqConfig.java
```

---

## Database Schema (Flyway)

### `V1__create_inventory_table.sql`

```sql
CREATE TABLE inventory (
    id               BIGSERIAL PRIMARY KEY,
    isbn             VARCHAR(255) NOT NULL UNIQUE,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity  INT NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0
);
```

- `version` enables optimistic locking — incremented on every update.

### `V2__create_reservation_table.sql`

```sql
CREATE TABLE reservation (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL,
    isbn          VARCHAR(255) NOT NULL,
    quantity      INT NOT NULL,
    status        VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_reservation_order_isbn UNIQUE (order_id, isbn)
);
```

- Unique constraint on `(order_id, isbn)` enforces idempotency at DB level.

---

## Application Ports

### Inbound Use Cases

```java
// ManageStockUseCase.java
Mono<InventoryItem> addStock(String isbn, int quantity);
Mono<InventoryItem> reduceStock(String isbn, int quantity);
Mono<InventoryItem> queryStock(String isbn);

// ReserveStockUseCase.java
Mono<InventoryDecision> reserveForOrder(OrderReserveRequest request);

// ReleaseStockUseCase.java
Mono<Void> releaseForOrder(UUID orderId);
```

### Outbound Ports

```java
// InventoryPort.java
Mono<InventoryItem> findByIsbn(String isbn);
Flux<InventoryItem> findAllByIsbn(List<String> isbns);
Mono<InventoryItem> save(InventoryItem item);
Flux<InventoryItem> saveAll(List<InventoryItem> items);

// ReservationPort.java
Flux<Reservation> findByOrderId(UUID orderId);
Mono<Reservation> save(Reservation reservation);

// InventoryEventPublisher.java
Mono<Void> publishInventoryDecision(InventoryDecision decision);
```

---

## Core Business Flows

### 5.1 Reserve Stock — All-or-Nothing (`@Transactional`)

```
OrderEventConsumer receives order.created
  → ReserveStockService.reserveForOrder(request)
      1. Load all InventoryItems for each ISBN in order
      2. Call item.reserve(quantity) for each — throws InsufficientStockException if any fail
      3. If any throw → transaction rolls back → publish REJECTED decision
      4. If all succeed → saveAll(items) + save each Reservation → publish RESERVED decision
```

**Transactional boundary:** entire step 2–4 runs in a single `@Transactional` reactive chain.

### 5.2 Concurrency — Optimistic Locking

- `inventory.version` column incremented on every `UPDATE`.
- On concurrent update conflict, R2DBC raises `OptimisticLockingFailureException`.
- `OrderEventConsumer` wraps the reserve call with a **retry with backoff** (`Retry.backoff(3, Duration.ofMillis(100))`).

### 5.3 Release Stock — Idempotent

```
OrderEventConsumer receives order.cancelled
  → ReleaseStockService.releaseForOrder(orderId)
      1. Load all Reservations by orderId with status = RESERVED
      2. If none found → no-op (already released or never reserved)
      3. For each: call item.release(quantity), update Reservation status → RELEASED
      4. saveAll updated items + reservations
```

Second invocation finds no `RESERVED` records → safe no-op.

### 5.4 Idempotency for Duplicate Reserve Events

If `OrderEventConsumer` receives the same `order.created` twice:
- Attempt to insert `Reservation` fails on unique constraint `(order_id, isbn)`.
- Catch `DataIntegrityViolationException` → log and return current `RESERVED` decision (no double-decrement).

---

## `build.gradle` Dependencies (additions over base Spring Boot)

```groovy
plugins {
    id 'nu.studer.jooq' version '9.0'
    id 'org.flywaydb.flyway' version '11.14.1'
    // ... spring boot, spotless, etc.
}

sourceSets {
    main {
        java {
            srcDirs += 'src/main/generated-jooq'
        }
    }
}

configurations {
    jooqGenerator
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-r2dbc'
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-flyway'
    implementation 'org.flywaydb:flyway-database-postgresql'
    implementation 'org.springframework.cloud:spring-cloud-stream-binder-kafka'
    implementation 'org.springframework:spring-jdbc'       // required by jOOQ reactive bridge
    implementation 'org.jooq:jooq:3.19.30'

    jooqGenerator 'org.postgresql:postgresql'

    runtimeOnly 'org.flywaydb:flyway-core'
    runtimeOnly 'org.postgresql:postgresql'
    runtimeOnly 'org.postgresql:r2dbc-postgresql'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:r2dbc'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'org.springframework.cloud:spring-cloud-stream'
    testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
}
```

### jOOQ code generation

```groovy
flyway {
    url = 'jdbc:postgresql://localhost:5434/polardb_inventory'
    user = 'user'
    password = 'password'
    baselineOnMigrate = true
}

jooq {
    version = '3.19.30'
    configurations {
        main {
            generateSchemaSourceOnCompilation = false
            generationTool {
                jdbc {
                    driver = 'org.postgresql.Driver'
                    url = 'jdbc:postgresql://localhost:5434/polardb_inventory'
                    user = 'user'
                    password = 'password'
                }
                generator {
                    database {
                        name = 'org.jooq.meta.postgres.PostgresDatabase'
                        includes = 'public.inventory|public.reservation'
                        inputSchema = 'public'
                    }
                    generate {
                        deprecated = false
                        records = true
                        pojos = false
                        fluentSetters = true
                        javaTimeTypes = true
                    }
                    target {
                        packageName = 'com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq.generated'
                        directory = 'src/main/generated-jooq'
                    }
                }
            }
        }
    }
}

tasks.named('generateJooq') {
    dependsOn tasks.named('flywayMigrate')
    allInputsDeclared = true
    inputs.files(fileTree('src/main/resources/db/migration')).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file('build.gradle').withPathSensitivity(PathSensitivity.RELATIVE)
}
```

Spotless plugin config identical to `order-service` (exclude `src/main/generated-jooq/**`).

---

## REST API (Inbound Web Adapter)

| Method | Path | Description |
|---|---|---|
| `GET` | `/inventory/{isbn}` | Query stock for a single ISBN |
| `GET` | `/inventory` | Query stock for multiple ISBNs (`?isbn=...&isbn=...`) |
| `POST` | `/inventory/{isbn}/adjust` | Admin — add or reduce stock (signed `delta`) |

---

## Kafka Topics

| Topic | Direction | Key | Event types |
|---|---|---|---|
| `order-events` | Inbound (consume) | `orderId` | `order.created`, `order.cancelled` |
| `inventory-events` | Outbound (publish) | `orderId` | `inventory.reserved`, `inventory.rejected` |

---

## Verification Plan

### Step 1 — Domain Unit Tests (no Spring context)
- `InventoryItemTest`: verify `reserve`, `release`, `adjust` invariants including boundary/error cases.
- `ReservationTest`: verify status transitions.
- → **verify:** all tests green, no Spring imports in test class.

### Step 2 — Application Service Tests (mocked ports)
- `ReserveStockServiceTest`: mock `InventoryPort`, `ReservationPort`, `InventoryEventPublisher`. Test all-or-nothing rollback path and success path.
- `ReleaseStockServiceTest`: test idempotent no-op when no RESERVED records found.
- → **verify:** tests green, zero DB/Kafka startup required.

### Step 3 — jOOQ Code Generation
- Run Flyway migrations against a local/Testcontainer Postgres to produce generated sources.
- → **verify:** `./gradlew generateJooq` succeeds; `generated/tables/Inventory.java` + `generated/tables/Reservation.java` present.

### Step 4 — Persistence Adapter Integration Tests (Testcontainers + R2DBC + jOOQ)
- Bring up PostgreSQL via Testcontainers.
- `JooqInventoryRepositoryImplTest`: test `findByIsbn`, `findAllByIsbn`, `save` with optimistic locking (`version` conflict raises `OptimisticLockingFailureException`).
- `JooqReservationRepositoryImplTest`: test unique constraint violation on duplicate `(order_id, isbn)` → `DataIntegrityViolationException`.
- → **verify:** `./gradlew test -Dspring.profiles.active=integration` green.

### Step 5 — Web Slice Tests
- `InventoryControllerTest`: `@WebFluxTest`, mock use cases. Verify request validation and response mapping.
- → **verify:** tests green without Kafka or DB.

### Step 6 — Messaging Integration Tests (Testcontainers + Kafka)
- `OrderEventConsumerTest`: publish `order.created` to Kafka testcontainer, assert `inventory-events` receives `inventory.reserved`.
- Test duplicate `order.created` — assert no double-decrement.
- → **verify:** tests green with Kafka + Postgres testcontainers.

---

## Open Questions / Review Points

> **Q1 — Stack confirmation:** Plan uses WebFlux + R2DBC (reactive). Confirm this is preferred over Spring MVC + JDBC (like `catalog-service`).

> **Q2 — Schema review:** `inventory` table with `version` column for optimistic locking, `reservation` table with unique `(order_id, isbn)` — any structural changes needed?

> **Q3 — Retry policy:** Retry on `OptimisticLockingFailureException` — 3 attempts with 100ms exponential backoff. Acceptable, or should we use a dead-letter topic after exhaustion?

> **Q4 — Event schema:** What fields should `inventory.reserved` and `inventory.rejected` events carry? Minimum: `orderId`, `status`, `reason` (on rejection), `timestamp`.

---

## Implementation Order

```
1. Scaffold service — build.gradle (jooq plugin + deps), Application class, Flyway migrations   → verify: bootRun starts
2. Domain model — InventoryItem, Reservation, exceptions                                         → verify: domain unit tests green
3. Application ports + service stubs                                                              → verify: app service tests green (mocked ports)
4. jOOQ code generation — run generateJooq, commit generated sources                             → verify: generated/tables/Inventory.java + Reservation.java present
5. Persistence adapter — JooqInventoryRepositoryImpl + JooqReservationRepositoryImpl + mappers   → verify: persistence integration tests green
6. Bootstrap JooqConfig — DSLContext + ReactiveTransactionManager beans                          → verify: context loads
7. Web adapter — InventoryController                                                              → verify: web slice tests green
8. Messaging inbound — OrderEventConsumer                                                         → verify: consumer integration test green
9. Messaging outbound — KafkaInventoryEventPublisher                                              → verify: full messaging flow test green
10. Wire everything in bootstrap + config                                                          → verify: full @SpringBootTest + Testcontainers green
```
