# AGENTS

## Mission
- Build and operate Spring bookstore services (`catalog-service`, `order-service`, `config-service`) with deterministic local and CI behavior.
- Favor correctness, reproducibility, and incremental changes over clever shortcuts.
- Keep runtime config centralized through Config Server and `config/` to avoid drift.
- Run the smallest useful verification for every change and document any skipped checks.
- Keep this file current whenever toolchains, workflows, or service boundaries change.

## Repository Layout
- `catalog-service/`: REST catalog API, Spring MVC + Spring Data JDBC + Flyway.
- `order-service/`: reactive order API, Spring WebFlux + Spring Data R2DBC + Flyway.
- `config-service/`: Spring Cloud Config Server.
- `config/`: config data consumed by Config Server (per service/profile YAML files).
- `polar-deployment/docker/`: Docker Compose stack.
- `polar-deployment/kubernetes/local/`: local Kubernetes manifests (currently Postgres only).
- No monorepo Gradle root build; each service is a standalone Gradle project with its own `./gradlew`.

## Runtime and Configuration
- Service defaults: config-service `8888`, catalog `9001`, order `9002`.
- `catalog-service` and `order-service` import config from `http://localhost:8888`.
- Local DB defaults:
  - catalog: `jdbc:postgresql://localhost:5432/polardb_catalog`
  - order: `r2dbc:postgresql://localhost:5433/polardb_order`
- Prefer editing `config/*.yml` for shared runtime behavior instead of hardcoding service-local values.
- Never commit real secrets; use environment variables and keep `.env*` ignored.

## Toolchain and Versions
- Java 21 via Gradle toolchains.
- Spring Boot `4.0.3` across all services.
- Spring Cloud BOM `2025.1.0`.
- Testcontainers BOM `1.19.7` in services that run container-backed tests.
- Spotless `6.25.0` is enabled in `catalog-service` and `order-service`.

## Build and Run Commands
- Per-service builds:
  - `cd catalog-service && ./gradlew clean build`
  - `cd order-service && ./gradlew clean build`
  - `cd config-service && ./gradlew clean build`
- Per-service run:
  - `cd config-service && ./gradlew bootRun`
  - `cd catalog-service && ./gradlew bootRun`
  - `cd order-service && ./gradlew bootRun`
- Root Makefile shortcuts:
  - `make build`, `make test`, `make clean`
  - `make run-config`, `make run-catalog`, `make run-order`
  - `make infra-up`, `make infra-down`, `make compose-up`, `make compose-down`
  - `make k8s-up`, `make k8s-down`, `make k8s-status`
  - `make skaffold-dev|skaffold-run|skaffold-delete` (requires `skaffold.yml`)

## Testing Strategy
- Use JUnit 5 and Spring test slices where possible; reserve full `@SpringBootTest` for cross-layer behavior.
- Keep tests deterministic and isolated by profile.
- Catalog tests may use JDBC Testcontainers profile (`integration`) and require Docker.
- Order tests may use R2DBC/Testcontainers stack and require Docker.
- For single tests, run from the target service directory using `./gradlew test --tests 'package.ClassName[.method]'`.

## Formatting and Style
- Run Spotless where configured:
  - `cd catalog-service && ./gradlew spotlessApply spotlessCheck`
  - `cd order-service && ./gradlew spotlessApply spotlessCheck`
- `config-service` has no Spotless plugin yet; keep style consistent with existing code.
- Prefer constructor injection, explicit imports, and thin controllers with domain logic in `domain`.

## API and Layer Conventions
- Controllers live in `web` packages and use REST resource naming (e.g., `/books`, `/orders`).
- Keep business rules in service/domain layer; controllers orchestrate request/response mapping.
- Use dedicated exception handling via `@RestControllerAdvice` where present.
- Keep validation with Jakarta Bean Validation annotations close to request/domain models.

## Persistence Conventions
- Catalog uses Spring Data JDBC; Flyway migrations under `catalog-service/src/main/resources/db/migration`.
- Order uses Spring Data R2DBC and Flyway; keep schema evolution incremental and versioned.
- Add migrations as `V<next>__description.sql`; do not edit historical migration files.

## CI/CD Expectations
- Workflows live in `.github/workflows/`:
  - `ci-config-pipeline.yml`
  - `ci-catalog-pipeline.yml`
  - `ci-order-pipeline.yml`
  - `ci-bookstore.yml` (orchestrator on `main` pushes)
- Keep local commands aligned with CI steps (`./gradlew build`, image packaging via `bootBuildImage`).

## Git and Agent Workflow
- Do not revert or rewrite unrelated user changes.
- Avoid destructive Git commands unless explicitly requested.
- Before commit/PR, run relevant tests and formatting checks for touched services.
- In commit/PR notes, mention:
  - touched service(s)
  - executed checks
  - whether Docker/Testcontainers tests were run
- Keep this document concise and accurate; prune outdated rules when adding new ones.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **spring-native-bookstore** (295 symbols, 382 relationships, 6 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## When Debugging

1. `gitnexus_query({query: "<error or symptom>"})` — find execution flows related to the issue
2. `gitnexus_context({name: "<suspect function>"})` — see all callers, callees, and process participation
3. `READ gitnexus://repo/spring-native-bookstore/process/{processName}` — trace the full execution flow step by step
4. For regressions: `gitnexus_detect_changes({scope: "compare", base_ref: "main"})` — see what your branch changed

## When Refactoring

- **Renaming**: MUST use `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` first. Review the preview — graph edits are safe, text_search edits need manual review. Then run with `dry_run: false`.
- **Extracting/Splitting**: MUST run `gitnexus_context({name: "target"})` to see all incoming/outgoing refs, then `gitnexus_impact({target: "target", direction: "upstream"})` to find all external callers before moving code.
- After any refactor: run `gitnexus_detect_changes({scope: "all"})` to verify only expected files changed.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Tools Quick Reference

| Tool | When to use | Command |
|------|-------------|---------|
| `query` | Find code by concept | `gitnexus_query({query: "auth validation"})` |
| `context` | 360-degree view of one symbol | `gitnexus_context({name: "validateUser"})` |
| `impact` | Blast radius before editing | `gitnexus_impact({target: "X", direction: "upstream"})` |
| `detect_changes` | Pre-commit scope check | `gitnexus_detect_changes({scope: "staged"})` |
| `rename` | Safe multi-file rename | `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` |
| `cypher` | Custom graph queries | `gitnexus_cypher({query: "MATCH ..."})` |

## Impact Risk Levels

| Depth | Meaning | Action |
|-------|---------|--------|
| d=1 | WILL BREAK — direct callers/importers | MUST update these |
| d=2 | LIKELY AFFECTED — indirect deps | Should test |
| d=3 | MAY NEED TESTING — transitive | Test if critical path |

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/spring-native-bookstore/context` | Codebase overview, check index freshness |
| `gitnexus://repo/spring-native-bookstore/clusters` | All functional areas |
| `gitnexus://repo/spring-native-bookstore/processes` | All execution flows |
| `gitnexus://repo/spring-native-bookstore/process/{name}` | Step-by-step execution trace |

## Self-Check Before Finishing

Before completing any code modification task, verify:
1. `gitnexus_impact` was run for all modified symbols
2. No HIGH/CRITICAL risk warnings were ignored
3. `gitnexus_detect_changes()` confirms changes match expected scope
4. All d=1 (WILL BREAK) dependents were updated

## CLI

- Re-index: `npx gitnexus analyze`
- Check freshness: `npx gitnexus status`
- Generate docs: `npx gitnexus wiki`

<!-- gitnexus:end -->


<claude-mem-context>
# Memory Context

# [spring-native-bookstore] recent context, 2026-04-28 3:42pm GMT+9

Legend: 🎯session 🔴bugfix 🟣feature 🔄refactor ✅change 🔵discovery ⚖️decision 🚨security_alert 🔐security_note
Format: ID TIME TYPE TITLE
Fetch details: get_observations([IDs]) | Search: mem-search skill

Stats: 50 obs (15,999t read) | 649t work | -2365% savings

### Apr 28, 2026
24 1:42p 🔵 Config Server lacks inventory-service configuration file
25 1:43p 🔵 Order-service config shows environment-specific URLs and Config Bus pattern
26 " 🔵 Inventory-service lacks OAuth2 resource server security configuration
27 " 🔵 Order-service has comprehensive Testcontainers integration tests
28 " 🔵 Order-service TestcontainersConfiguration uses Spring Boot @ServiceConnection for automatic database connection
29 1:44p 🔵 Inventory-service persistence adapters implement jOOQ with optimistic locking and reactive patterns
30 " 🔵 Inventory and order services use identical PersistenceConfig with R2DBC auditing enabled
31 " 🔵 Inventory and order services use identical JooqConfig for reactive jOOQ with R2DBC integration
32 " 🔵 OrderControllerTest demonstrates web slice testing pattern with OAuth2 security mocks
33 1:45p 🔵 Order-service messaging tests use TestChannelBinderConfiguration for Kafka-free integration testing
38 " 🔵 Inventory-service jOOQ generated code directory does not exist
39 " 🔵 Inventory-service jOOQ generated code exists in src/main/generated-jooq directory
46 " 🔴 Added pgcrypto extension migration to resolve gen_random_uuid dependency
34 1:46p 🔵 Inventory-service messaging layer fully implemented with Kafka event publisher and message records
35 " 🔵 Inventory-service has two Flyway migration scripts for inventory and reservation tables
36 " 🔵 Inventory-service database schema defines inventory and reservation tables with optimistic locking and unique constraints
37 " 🔵 Inventory-service migration uses gen_random_uuid() without pgcrypto extension creation
40 1:48p 🔵 Inventory-service build.gradle configures jOOQ 3.19.30 code generation with Flyway migration dependency
41 " 🔵 InventoryItem domain record implements immutable inventory management with validation
42 1:49p 🔵 ReserveStockService implements all-or-nothing stock reservation with transactional reactive pattern
43 " 🔵 StockAdjustmentRequest DTO defines admin stock adjustment input with validation
44 1:50p 🔵 Polar deployment Kubernetes infrastructure exists but inventory-service K8s manifests missing
45 " 🔵 Skaffold configuration only includes edge and dispatcher services, missing inventory/order/catalog manifests
47 1:52p 🔴 JooqReservationRepositoryImpl fixed DataIntegrityViolationException construction in onErrorMap
49 " 🟣 InventoryControllerTest converted from @WebFluxTest slice test to @SpringBootTest integration test
51 " 🔵 No disabledWithoutDocker configuration found in inventory-service or order-service test configurations
48 " 🔵 InventoryControllerTest compilation fails due to missing @WebFluxTest dependency
50 1:53p 🔵 Inventory-service tests fail with DockerClientProviderStrategy IllegalStateException
52 " ✅ JooqInventoryRepositoryImplTest configured with disabledWithoutDocker to skip when Docker unavailable
53 " 🔵 Inventory-service lacks scaffold pattern and has scattered environment configuration
54 1:56p 🔵 Examined JooqReservationRepositoryImpl implementation patterns
55 " 🔴 Re-applied Spotless indentation fix for method chain alignment
56 3:32p 🔴 Method chain indentation fix successfully applied
57 " 🔴 Spotless formatting verification passed successfully
58 " 🔴 Method chain indentation re-aligned in JooqReservationRepositoryImpl
59 3:33p 🔴 Spotless formatting validation confirmed successful
60 " 🔵 Inventory-service not found in deployment configuration files
61 3:34p 🔵 Confirmed: inventory-service absent from deployment orchestration
62 " 🔴 Spotless formatting compliance re-verified successfully
63 " 🔵 Deployment configuration gap confirmed for inventory-service
64 " 🔵 Final verification: inventory-service excluded from deployment pipeline
65 " 🔵 Makefile SERVICES variable excludes inventory-service
66 " 🔵 Config Server patterns identified from existing services
67 3:35p 🔵 Tiltfile excludes inventory-service from local development
68 " 🔵 Local development Tilt configuration confirmed missing inventory-service
69 " 🔵 Service Tiltfile patterns identified from order-service and catalog-service
70 " 🔵 Tiltfile service inclusion pattern re-verified
71 " 🔵 Tiltfile service configuration patterns re-confirmed
72 " 🔵 Order-service lacks inventory integration event handlers
73 3:36p 🔵 Order-service application.yml configuration patterns identified
</claude-mem-context>