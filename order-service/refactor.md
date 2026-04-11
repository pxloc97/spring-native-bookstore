# Order Service Refactor Plan

## Decisions

- Architecture style: Hombergs-style hexagonal architecture.
- Reactive model: keep reactive end-to-end at the application and adapter boundaries.
- Persistence stack: use `jOOQ + R2DBC`.
- Persistence abstraction: do not use `spring-data-r2dbc` repositories in the production path.
- Schema migration: keep Flyway.
- Messaging: Kafka is deferred for now. Keep an outbound event publisher port with a no-op adapter until messaging is implemented.

## Target Package Layout

```text
order-service/src/main/java/com/locpham/bookstore/orderservice/

  domain/
    model/
      Order.java                    ← framework-free, immutable
      OrderStatus.java
      BookSnapshot.java             ← domain view of catalog data
    exception/
      OrderNotFoundException.java

  application/
    port/
      in/
        SubmitOrderUseCase.java
        GetOrdersUseCase.java
        BuyFlashSaleBookUseCase.java
        MarkOrderDispatchedUseCase.java
      out/
        OrderCommandPort.java
        OrderQueryPort.java
        CatalogBookPort.java        ← returns Mono<BookSnapshot>
        OrderEventPublisherPort.java
    service/
      SubmitOrderService.java
      GetOrdersService.java
      BuyFlashSaleBookService.java
      MarkOrderDispatchedService.java
    command/
      SubmitOrderCommand.java
      BuyFlashSaleBookCommand.java
      MarkOrderDispatchedCommand.java
    query/
      GetOrdersQuery.java

  adapter/
    in/
      web/
        OrderController.java
        dto/
          OrderRequest.java
          FlashSaleRequest.java
          OrderResponse.java
        mapper/
          OrderWebMapper.java
      messaging/                     ← inbound messaging adapter
        OrderDispatchedConsumerAdapter.java
        OrderDispatchedMessage.java  ← adapter DTO

    out/
      persistence/
        jooq/
          JooqOrderRepositoryAdapter.java
          JooqOrderMapper.java
      catalog/
        CatalogWebClientAdapter.java
        CatalogClientConfig.java
        CatalogProperties.java
        BookDto.java                 ← external DTO (was Book.java)
      messaging/
        noop/
          NoOpOrderEventPublisher.java
        OrderAcceptedMessage.java    ← adapter DTO

  bootstrap/
    config/
      SecurityConfig.java
      PersistenceConfig.java
      JooqConfig.java
      WebClientConfig.java
      CatalogClientConfig.java
      MessagingConfig.java
```

## Dependency Rules

- `domain` must not depend on Spring, Reactor, jOOQ, WebClient, Kafka, or database annotations.
- `application` may expose `Mono` and `Flux`, but must not depend on WebClient, jOOQ, Kafka, or Spring Data repositories.
- `adapters` implement ports and may depend on framework details.
- `bootstrap` is only for wiring and configuration.

## Clarification About "Two Services"

There should not be two business services for the same use case flow.

For this project:

- Do not introduce a generic `OrderDomainService`.
- Do not introduce a generic `OrderApplicationService`.
- Use use-case-oriented services instead:
  - `SubmitOrderService`
  - `GetOrdersService`
  - `BuyFlashSaleBookService`
  - `MarkOrderDispatchedService`
- Each handler implements exactly one inbound port.
- Domain rules should live in `Order` unless there is a genuinely shared rule that does not belong to any entity or value object.

In short:

- `use case service` = one application use case, one orchestration boundary.
- `domain object` = owns business invariants and state transitions.
- `domain service` = rare escape hatch, not a default layer.

If you start with both a generic domain service and a generic application service, the design gets muddled very quickly.

## Current Problems To Remove

- Domain/application logic currently depends directly on HTTP client and messaging framework.
- Core repository abstraction currently depends on `ReactiveCrudRepository`.
- Domain model currently contains persistence annotations.
- Web layer currently talks to a service that mixes use case orchestration and infrastructure concerns.

## Refactor Phases

## Phase 1: Clean The Domain Model

Goal:

- Make domain types framework-free and shape all downstream ports.

Tasks:

- Move `Order` and `OrderStatus` under `domain/model/`.
- Remove all persistence annotations from `Order`.
- Add domain creation/transition methods:
  - `Order.accepted(isbn, title, price, quantity)`
  - `Order.rejected(isbn, quantity)`
  - `order.markDispatched()`
- Introduce `BookSnapshot` as a domain-facing view of catalog data (immutable value object).
- Convert `GetOrdersUseCase.kt` to `GetOrdersUseCase.java` for consistency.
- Keep all domain objects immutable.
- Add `OrderNotFoundException` exception.

Done when:

- `domain` compiles without Spring, Reactor, jOOQ, or database annotations.
- All domain types are pure Java records or enums.

## Phase 2: Freeze The Core Boundary (Ports & Use-Case Services)

Goal:

- Define clear inbound and outbound ports shaped by clean domain types.
- Separate `application` orchestration from infrastructure concerns.

Tasks:

- Create inbound ports in `application/port/in/`:
  - `SubmitOrderUseCase` → `Mono<Order> submitOrder(SubmitOrderCommand)`
  - `GetOrdersUseCase` → `Flux<Order> getOrders(GetOrdersQuery)`
  - `BuyFlashSaleBookUseCase` → `Mono<Order> buyFlashSaleBook(BuyFlashSaleBookCommand)`
  - `MarkOrderDispatchedUseCase` → `Mono<Order> markOrderDispatched(MarkOrderDispatchedCommand)`
- Create outbound ports in `application/port/out/`:
  - `OrderCommandPort` → save/update order
  - `OrderQueryPort` → load order(s)
  - `CatalogBookPort` → `Mono<BookSnapshot> loadBook(String isbn)`
  - `OrderEventPublisherPort` → `Mono<Void> publishOrderAccepted(Order)`
- Implement use-case-specific services (one per inbound port):
  - `SubmitOrderService implements SubmitOrderUseCase`
  - `GetOrdersService implements GetOrdersUseCase`
  - `BuyFlashSaleBookService implements BuyFlashSaleBookUseCase`
  - `MarkOrderDispatchedService implements MarkOrderDispatchedUseCase`
- Update `OrderController` to depend only on inbound ports.
- Provide a `NoOpOrderEventPublisher` adapter for now.
- Keep current behavior unchanged.

Done when:

- No application class depends directly on `WebClient`, `StreamBridge`, or Spring Data repository interfaces.
- Each use case has one implementation class with one clear responsibility.
- All ports are shaped by domain types, not infrastructure types.

## Phase 3: Refactor Catalog Adapter

Goal:

- Turn the current catalog HTTP client into a proper outbound adapter implementing `CatalogBookPort`.

Tasks:

- Rename `Book.java` to `BookDto.java` and move to `adapter/out/catalog/` (external DTO).
- Create `CatalogWebClientAdapter implements CatalogBookPort`:
  - Accepts `String isbn`, returns `Mono<BookSnapshot>`.
  - Handles timeout, retry, and error mapping.
  - Maps `BookDto` → `BookSnapshot`.
- Keep `CatalogClientConfig.java` and `CatalogProperties.java` in `adapter/out/catalog/`.
- Remove direct `BookClient` usage from application layer.

Done when:

- Application layer no longer imports `BookClient` or `BookDto`.
- `CatalogBookPort` is the only catalog dependency in application.

## Phase 4: Persistence Adapter (jOOQ + R2DBC)

Goal:

- Use jOOQ as the only production persistence adapter API.

Tasks:

- Remove `ReactiveCrudRepository` usage from the production code path.
- Add jOOQ dependencies and code generation.
- Generate jOOQ classes from the Flyway-managed schema.
- Configure jOOQ to use the R2DBC `ConnectionFactory`.
- Implement `JooqOrderRepositoryAdapter implements OrderCommandPort, OrderQueryPort`:
  - Handles all order persistence operations.
  - Maps between jOOQ records and domain `Order` objects.
- Add `JooqOrderMapper` for record ↔ domain conversions.

Rules:

- jOOQ generated classes must not leak into `application` or `domain`.
- R2DBC remains the reactive connection and transaction foundation.
- Flyway remains the schema source of truth.

Done when:

- Order reads and writes no longer depend on Spring Data R2DBC repositories.

## Phase 5: Web Adapter Cleanup

Goal:

- Keep controller and DTO layer thin.

Tasks:

- Keep validation annotations in request DTOs.
- Map request DTOs to application commands via `OrderWebMapper`.
- Optionally map domain `Order` to response DTOs instead of returning domain directly.
- Do not place business logic in controllers.
- Update `OrderController` to call inbound use-case ports.

Done when:

- Controllers only validate, map, and call inbound ports.

## Phase 6: Inbound Messaging Adapter

Goal:

- Prepare the inbound messaging boundary for future Kafka integration.

Tasks:

- Create `OrderDispatchedConsumerAdapter` in `adapter/in/messaging/`:
  - Consumes `OrderDispatchedMessage` (adapter DTO).
  - Calls `MarkOrderDispatchedUseCase` to update order state.
- Move `OrderDispatchedMessage` to `adapter/in/messaging/`.
- Move `OrderAcceptedMessage` to `adapter/out/messaging/`.
- Create `MessagingConfig.java` in `bootstrap/config/` to wire consumers.

Done when:

- Messaging can be swapped between no-op and Kafka without changing application logic.

## Phase 7: Transaction Boundaries

Goal:

- Keep use case execution reactive and transactional.

Tasks:

- Define transaction boundaries in the use-case services.
- Prefer `TransactionalOperator` for explicit reactive transaction control.
- Verify rollback behavior with integration tests.

Done when:

- Submit order and state transition flows are covered by reactive transaction tests.

## Testing Plan

## Domain Tests

- Pure unit tests for order creation and state transitions.

## Application Tests

- Mock outbound ports.
- Verify orchestration, fallback behavior, and event publishing decisions.
- Use `StepVerifier`.

## Persistence Tests

- Use Testcontainers PostgreSQL.
- Run Flyway migrations.
- Test `JooqOrderRepositoryAdapter` directly.
- Verify read/write mapping, auditing behavior, and transaction rollback.

## Web Tests

- Use `@WebFluxTest`.
- Mock inbound use cases only.

## Build And Dependency Tasks

- Remove `spring-boot-starter-data-r2dbc` if it is only present for repository abstraction and not needed by any remaining feature.
- Keep `spring-boot-starter-r2dbc`.
- Add jOOQ runtime and code generation setup.
- Review auditing support because current auditing is coupled to Spring Data R2DBC.

## Key Architectural Corrections (vs. Initial Draft)

### 1. **Adapter Layer Structure: `in/` vs `out/` Separation**

- **Before**: All adapters mixed in single `adapters/` folder.
- **After**: Adapters split by driving vs. driven direction:
  - `adapter/in/` → inbound adapters (web, messaging consumers)
  - `adapter/out/` → outbound adapters (persistence, catalog client, messaging publishers)
- **Why**: Clarifies dependency flow and makes it obvious which adapters drive vs. are driven by the core.

### 2. **Event Message DTOs Belong in Adapter Layer**

- **Before**: `OrderAcceptedMessage`, `OrderDispatchedMessage` in generic `event/` package.
- **After**: 
  - `OrderAcceptedMessage` → `adapter/out/messaging/` (outbound DTO)
  - `OrderDispatchedMessage` → `adapter/in/messaging/` (inbound DTO)
- **Why**: These are infrastructure concerns, not domain or application logic. Keeps domain/application clean.

### 3. **Book Migration: Domain vs. External DTO**

- **Before**: `Book` record in `core.domain.entity.bookSnapshot` (looks like domain entity).
- **After**:
  - `BookDto` in `adapter/out/catalog/` (external DTO from catalog-service)
  - `BookSnapshot` in `domain/model/` (domain-facing immutable value object)
  - `CatalogWebClientAdapter` maps `BookDto` → `BookSnapshot`
- **Why**: Separates external contract (catalog-service API) from domain model, preventing external changes from breaking domain logic.

### 4. **Phase Ordering: Domain First, Then Ports**

- **Before**: Phase 1 = ports, Phase 2 = domain cleanup.
- **After**: Phase 1 = domain cleanup, Phase 2 = ports.
- **Why**: Ports must be shaped by clean domain types. If domain is dirty, ports inherit the mess. Build from inside out.

### 5. **Inbound Messaging Adapter Was Missing**

- **Before**: Only outbound `NoOpOrderEventPublisher` mentioned.
- **After**: Added Phase 6 for inbound `OrderDispatchedConsumerAdapter`.
- **Why**: Hombergs hexagon has both inbound and outbound adapters. Messaging is bidirectional.

### 6. **Consolidated Config Classes**

- **Before**: Generic `BeanWiringConfig.java` for all wiring.
- **After**: Separate config classes per adapter:
  - `PersistenceConfig` → persistence wiring
  - `JooqConfig` → jOOQ setup
  - `WebClientConfig` → HTTP client setup
  - `CatalogClientConfig` → catalog adapter wiring
  - `MessagingConfig` → messaging adapter wiring
  - `SecurityConfig` → security setup
- **Why**: Avoids God config, makes dependencies explicit, easier to test and modify.

### 7. **Kotlin File Removed from Target Layout**

- **Before**: `GetOrdersUseCase.kt` in Java project.
- **After**: Convert to `GetOrdersUseCase.java`.
- **Why**: Consistency; no mixed language in single project without explicit multi-language build setup.

## Open Design Questions

- Should audit fields remain part of the domain model, or move to persistence mapping only?
- Should `OrderResponse` expose audit metadata?
- Should flash-sale be a first-class use case or remain a temporary endpoint?
- Should optimistic locking remain, and if yes, how should version be modeled in the domain?

## Recommended Execution Order

1. **Phase 1**: Clean domain model (`Order`, `OrderStatus`, `BookSnapshot`, exceptions).
2. **Phase 2**: Define ports and create use-case services (shaped by clean domain types).
3. **Phase 3**: Refactor catalog adapter (`BookDto`, `CatalogWebClientAdapter`).
4. **Phase 4**: Introduce jOOQ code generation and replace Spring Data R2DBC.
5. **Phase 5**: Refactor web adapter (controllers, DTOs, mappers).
6. **Phase 6**: Prepare inbound messaging adapter structure (no-op for now).
7. **Phase 7**: Add transaction boundaries and reactive transaction tests.
8. Refactor all tests around the new layers (unit, integration, web slices).
