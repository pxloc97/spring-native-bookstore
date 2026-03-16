# Spring Native Bookstore

Spring Native Bookstore is a multi-service Spring Boot project with a central Config Server and domain services for catalog and ordering.

## Services

- `config-service` (port `8888`): Spring Cloud Config Server serving shared config from `config/`
- `catalog-service` (port `9001`): Catalog REST API backed by PostgreSQL + Flyway + Spring Data JDBC
- `order-service` (port `9002`): Order API backed by PostgreSQL + Flyway + Spring Data R2DBC/WebFlux

## Tech Stack

- Java 21 (Gradle toolchains)
- Spring Boot `4.0.3`
- Spring Cloud `2025.1.0`
- PostgreSQL `13.4`
- Testcontainers for integration tests

## Repository Layout

```text
.
|- catalog-service/
|- config-service/
|- order-service/
|- config/
`- polar-deployment/
```

## Prerequisites

- JDK 21
- Docker (for PostgreSQL and Testcontainers-backed tests)
- GNU Make
- `kubectl` (for Kubernetes targets)
- `skaffold` (optional, for Skaffold targets)

## Quick Start

1. Start local infrastructure:

   ```bash
   make infra-up
   ```

2. Run config-service (required by app services):

   ```bash
   make run-config
   ```

3. In separate terminals, run app services:

   ```bash
   make run-catalog
   make run-order
   ```

## Common Commands

Use `make help` to list all available targets.

```bash
make build            # Build all services
make test             # Run all tests
make clean            # Clean all build outputs
make format           # Apply Spotless (catalog + order)
make lint             # Spotless check (catalog + order)
make infra-up         # Start local PostgreSQL containers
make infra-down       # Stop local PostgreSQL containers
make k8s-up           # Apply local Kubernetes Postgres manifests
make k8s-down         # Delete local Kubernetes Postgres manifests
make skaffold-dev     # Run Skaffold dev loop (if skaffold.yml exists)
```

## Service-Level Commands

Examples:

```bash
make build-catalog
make test-order
make run-config
```

## API Endpoints

### catalog-service (`http://localhost:9001`)

- `GET /` - returns configured greeting
- `GET /books` - list all books
- `GET /books/{isbn}` - get one book by ISBN
- `POST /books` - create a book (returns `201`)
- `PUT /books/{isbn}` - update a book
- `DELETE /books/{isbn}` - delete a book (returns `204`)

Create a book:

```bash
curl -X POST http://localhost:9001/books \
  -H 'Content-Type: application/json' \
  -d '{
    "isbn": "1234567890",
    "title": "Cloud Native Spring in Action",
    "author": "Thomas Vitale",
    "price": 49.90,
    "publisher": "Manning"
  }'
```

### order-service (`http://localhost:9002`)

- `GET /orders` - list all orders
- `POST /orders` - submit an order

Submit an order:

```bash
curl -X POST http://localhost:9002/orders \
  -H 'Content-Type: application/json' \
  -d '{"isbn":"1234567890","quantity":1}'
```

### config-service (`http://localhost:8888`)

- `GET /{application}/{profile}` - fetch app config (for example `catalog-service/default`)
- `GET /{application}/{profile}/{label}` - fetch app config from a specific Git label/branch

Example:

```bash
curl http://localhost:8888/catalog-service/default
```

## Kubernetes and Skaffold

- `make k8s-up` applies local PostgreSQL manifests from `polar-deployment/kubernetes/local/`
- `make k8s-down` removes those manifests
- `make k8s-status` shows deployments/services/pods in the current namespace
- `make skaffold-dev`, `make skaffold-run`, and `make skaffold-delete` require a `skaffold.yml` file at repo root (or override with `SKAFFOLD_FILE=<path>`)

## CI/CD

GitHub Actions workflow definitions are in `.github/workflows/`:

- `ci-config-pipeline.yml`
- `ci-catalog-pipeline.yml`
- `ci-order-pipeline.yml`
- `ci-bookstore.yml` (aggregates the service pipelines)

## Notes

- There is no monorepo Gradle root build; each service uses its own `./gradlew`.
- `config-service` points to this repository's `config/` directory through Spring Cloud Config.
- Keep config changes in `config/*.yml` for shared runtime configuration.
