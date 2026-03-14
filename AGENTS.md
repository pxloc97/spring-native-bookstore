# AGENTS

## Mission
- Provide Spring-based bookstore services (catalog-service and config-service) with reliable builds, predictable tests, and production-ready configs.
- Keep developer/agent workflows deterministic so CI/CD mirrors local runs.
- Prioritize correctness and reproducibility over cleverness; follow established service boundaries.
- Maintain parity between local configs and Config Server defaults to avoid drift.
- Prefer incremental, well-tested changes; document assumptions inline only when non-obvious.
- Remember that agents after you rely on this file—update it whenever tooling or conventions change.

## Repository Layout
- `catalog-service/` holds the RESTful catalog app (Spring Boot, Data JDBC, Testcontainers-backed tests).
- `config-service/` is a Spring Cloud Config Server supplying shared configuration.
- `config/` contains central configuration data served by config-service (YAML files per service/profile).
- Each service directory is a standalone Gradle project with its own wrapper (`./gradlew`).
- Tests, resources, and Docker/Testcontainers assets live inside each service folder; keep cross-service coupling minimal.
- There is no monorepo-level Gradle build; always operate from the specific service root.

## Runtime Profiles & Config
- Default profile pulls configuration from config-service at `http://localhost:8888`; ensure it runs when exercising catalog-service locally.
- `application-integration.yml` activates the `integration` profile with a JDBC Testcontainers URL (`jdbc:tc:postgresql:13.4:///`).
- For seeded demo data, use the `test-data` Spring profile (see `BookDataLoader`).
- Sensitive properties are expected from Config Server or environment variables; never hardcode credentials in source.
- Auditing fields (`@CreatedDate`, `@LastModifiedDate`) require `DataConfig` to be imported when using Spring slices.
- Keep profile-specific overrides inside each service's `src/main/resources` hierarchy or Config Server repo.

## Toolchain & Versions
- Java 21 via Gradle toolchains; ensure local JDK 21 is installed even though Gradle manages compilation.
- Spring Boot 4.0.3 across services; align new dependencies to this baseline.
- Spring Cloud BOM version 2025.1.0; update both services simultaneously if upgrading.
- Testcontainers BOM 1.19.7 for catalog-service integration tests.
- Spotless 6.25.0 with Google Java Format (AOSP flavor) enforces formatting in catalog-service.
- No Cursor (.cursor) or GitHub Copilot instruction files exist; this AGENTS.md is the canonical automation guide.

## Build & Boot Commands
- Catalog service: `cd catalog-service && ./gradlew clean build` for a full compile/test/package cycle.
- Config service: `cd config-service && ./gradlew clean build`.
- Skip tests when necessary with `./gradlew build -x test`, but document the reason in PRs.
- Run catalog-service locally: `./gradlew bootRun` (set `SPRING_PROFILES_ACTIVE` or `--args='--spring.profiles.active=dev'`).
- Run config-service locally: `./gradlew bootRun --args='--server.port=8888'` (ports configured in application.yml).
- Use Gradle's `--continuous` mode sparingly; prefer explicit invocations referenced above.

## Testing Strategy
- Unit and slice tests live under each module's `src/test/java` and rely on JUnit 5.
- Catalog-service uses Testcontainers (via JDBC URL) for repository/integration tests—Docker is required for these cases.
- Web layer tests employ `@JsonTest` and `@WebMvcTest`/`WebTestClient`; reuse those patterns for new controllers.
- Config-service currently has lightweight Spring Boot tests; expand similarly when adding features.
- Use `@ActiveProfiles("integration")` for database-backed tests to isolate from local Postgres.
- Favor Spring Boot test slices (`@DataJdbcTest`, `@JsonTest`, etc.) for speed, falling back to `@SpringBootTest` only when necessary.

## Running Single Tests
- Catalog-service example: `./gradlew test --tests 'com.locpham.bookstore.catalogservice.domain.BookRepositoryJdbcTests'`.
- Run a single method with wildcards: `./gradlew test --tests 'com.locpham.*BookControllerMvcTests.whenGetBooksThenReturn200'`.
- For config-service, mirror the command from its directory (`./gradlew test --tests '...ClassName'`).
- Use `./gradlew :catalog-service:test --tests 'pattern'` only if you ever add a composite Gradle build (not present today).
- Re-run failed tests faster with the `--rerun-tasks` flag when Gradle caches results.
- Keep IDE-specific runners in sync with Gradle output; rely on Gradle as the source of truth for CI parity.

## Linting & Formatting
- Catalog-service enforces Spotless; run `./gradlew spotlessApply` before committing automated formatting changes.
- Use `./gradlew spotlessCheck` in CI scripts or pre-commit hooks to guard formatting regressions.
- Config-service currently lacks Spotless; follow the same style manually until tooling is added.
- Avoid auto-formatters that conflict with Google Java Format AOSP; rely on Spotless output as the final arbiter.
- Keep imports organized automatically via Spotless (`importOrder()` plus `removeUnusedImports()`).
- Ensure files end with a single newline and contain no trailing whitespace.

## Dependency Management
- Declare dependencies in each module's `build.gradle`; keep versions centralized via the provided `ext` properties.
- When adding libraries used in both services, align versions and BOM entries to prevent drift.
- Use `implementation` vs `runtimeOnly` vs `testImplementation` consistently (see catalog-service example for guidance).
- Prefer Spring Boot starters over raw dependencies when possible for consistent auto-configuration.
- Document any new third-party addition in commit/PR descriptions, especially if it affects licensing or deployment size.
- Avoid editing Gradle wrapper versions unless coordinating across all services.

## Coding Style Fundamentals
- Java record `Book` demonstrates the preferred immutable domain model—favor records or final classes for DTOs.
- Prefer constructor injection for Spring components; avoid field injection except in tests where brevity matters.
- Use `var` for local variables when the type is evident from the initializer, mirroring existing tests and controllers.
- Keep methods focused and side-effect aware; service methods typically delegate to repositories and throw domain exceptions.
- Use `Optional` for repository lookups instead of returning `null`.
- Keep domain-specific logic inside the `domain` package; controllers should remain thin delegators.

## Imports & Formatting Nuances
- Static imports are encouraged for `Assertions.assertThat` and other repeated JUnit helpers to improve readability.
- Keep import blocks grouped: java.*, javax/jakarta.*, third-party, org.springframework.*, project packages (Spotless handles this order).
- Avoid wildcard imports; rely on explicit class imports only.
- Annotate records and DTO fields vertically, one annotation per line as shown in `Book`.
- Chain fluent assertions each on its own line for clarity (see `BookRepositoryJdbcTests`).
- Wrap JSON strings in text blocks (`"""`) for readability, aligning indentation with existing tests.

## Types & Naming
- Package names follow `com.locpham.bookstore.<service>.<layer>`; maintain this hierarchy.
- Classes use UpperCamelCase; methods and variables use lowerCamelCase; constants prefer `UPPER_SNAKE_CASE` though currently rare.
- Avoid Hungarian notation; rely on descriptive names (`bookIsbn`, `expectedBook`).
- Domain exceptions end with `Exception` and live under the `domain` package.
- Repositories extend Spring Data interfaces and should be named `XRepository`.
- Keep DTO and request/response naming aligned with domain models to reduce mapping overhead.

## Error Handling & Validation
- Throw domain-specific exceptions (`BookNotFoundException`, `BookAlreadyException`) from service layer methods.
- Keep `@RestControllerAdvice` centralized (`BookControllerAdvice`) to convert exceptions into HTTP responses.
- Validation uses Jakarta Bean Validation annotations directly on the `Book` record; reuse these annotations for new fields.
- For request validation errors, propagate field-level messages exactly as in `handleValidationExceptions`.
- Use `HttpStatus.UNPROCESSABLE_ENTITY` for business rule violations and `HttpStatus.NOT_FOUND` for missing resources.
- Log unexpected exceptions within controller advice before rethrowing/propagating when new handlers are added.

## Web Layer Conventions
- Controllers reside in `web` packages and map to plural nouns (e.g., `/books`).
- Use `@ResponseStatus` on controller methods to declare non-200 responses (created/no-content) rather than returning `ResponseEntity` when simple.
- Keep request bodies as domain records when validation requirements match persistence models; introduce dedicated DTOs only when shapes diverge.
- Prefer `WebTestClient` or MockMvc-based tests for controllers depending on the stack (currently WebTestClient for `CatalogServiceApplicationTests`).
- Document new endpoints via Spring REST Docs/OpenAPI if/when added; currently tests serve as living specs.
- Stick to REST semantics: GET for reads, POST for create, PUT for replace, DELETE for removal.

## Data & Persistence Layer
- Data access uses Spring Data JDBC; keep aggregate roots simple and avoid complex joins or lazy loading expectations.
- `BookRepository` extends `CrudRepository`; additional queries should follow Spring Data naming conventions or annotated SQL.
- Use `JdbcAggregateTemplate` in tests or advanced scenarios where repository abstractions fall short.
- Remember to update `Book.build` whenever the record signature changes—it seeds demo data and tests.
- Flyway manages catalog-service schema via `src/main/resources/db/migration` (V1 seeds the default book; V2 adds the `publisher` column); add new `V<next>__description.sql` files for future changes.
- Reuse `DataConfig` auditing setup for any new service requiring created/modified timestamps.

## Testing Conventions & Fixtures
- Naming: `ClassNameTests` with descriptive method names (`whenXThenY`) as seen throughout catalog-service.
- Favor AssertJ for fluent assertions; use `usingRecursiveComparison()` to compare aggregates with auditing fields.
- Keep test data deterministic; avoid relying on wall-clock time except when verifying auditing fields explicitly.
- Profile-based tests (`@ActiveProfiles("integration")`) should insert data through Spring components rather than raw SQL when possible.
- Maintain separation between slice tests (MVC/JSON/Data) and full `@SpringBootTest` scenarios.
- Seed data through builders/factory methods (e.g., `Book.build`) to minimize duplication.

## Configuration & Secrets
- Local credentials in `application.yml` are placeholders; override via environment variables or Config Server for real deployments.
- Config-service should serve YAML files stored under `config/`; update those files instead of editing service-local `application.yml` when possible.
- Never commit actual secrets; rely on `.gitignore` for `.env` or credentials files (verify before adding new artifacts).
- When integration tests need credentials, prefer Testcontainers' JDBC URL form (already implemented) over manual username/password storage.
- Document any new required environment variable within this AGENTS file and README updates.
- Keep `application.yml` minimal and delegate feature toggles to Config Server-managed properties.

## Git & Workflow Expectations
- Do not mutate user-authored changes in unrelated files; respect current working tree instructions.
- Avoid rewriting history (`git commit --amend`, force push) unless explicitly instructed by the user.
- Run relevant tests and `spotlessCheck` before committing; mention skipped steps in commit messages when unavoidable.
- Use descriptive branch names per feature/service (`feature/catalog-add-search`).
- Reference modules explicitly in commits/PRs to aid reviewers (e.g., "catalog: add JDBC repo test").
- Always note whether integration tests relying on Docker/Testcontainers were executed.

## Agent Tips & Miscellaneous
- Always operate from the service directory before running Gradle commands to avoid wrapper confusion.
- When editing generated files, prefer `apply_patch` or structured editors; avoid ad-hoc shell redirection.
- No repository-specific skills/plugins are registered; rely on general tooling instructions near the top of each conversation.
- Respect ASCII preference for files; introduce Unicode only when unavoidable.
- Keep this AGENTS file ~150 lines by pruning obsolete sections whenever you add new ones.
- If you find or add Cursor/Copilot rules later, summarize them in a new section within this file.
