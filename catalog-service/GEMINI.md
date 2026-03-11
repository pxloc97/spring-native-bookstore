# GEMINI.md - Catalog Service

## Project Overview
The `catalog-service` is a Spring Boot application that manages a catalog of books for the `spring-native-bookstore` system. It provides a RESTful API for viewing, adding, editing, and deleting books.

- **Main Technologies:**
  - Java 21
  - Spring Boot 4.0.3
  - Gradle
  - Spotless (Google Java Format - AOSP style)
  - In-memory storage (ConcurrentHashMap)

- **Architecture:**
  - **Domain-Driven Design (Lite):**
    - `domain`: Contains the core domain model (`Book` record), business logic (`BookService`), and repository interface (`BookRepository`).
    - `web`: Contains the REST controller (`BookController`) and global exception handler (`BookControllerAdvice`).
  - **Persistence:** Currently uses an in-memory implementation (`InMemoryBookRepository`).

## Building and Running

### Key Commands
- **Run the application:** `./gradlew bootRun` (runs on port 9001)
- **Run tests:** `./gradlew test`
- **Apply formatting:** `./gradlew spotlessApply`
- **Check formatting:** `./gradlew spotlessCheck`
- **Build the JAR:** `./gradlew build`

### Configuration
- **Port:** 9001 (configured in `src/main/resources/application.yml`)
- **Tomcat:** Configured with a 2s connection timeout and custom thread pool settings.

## Development Conventions

### Coding Style
- **Formatting:** Use `./gradlew spotlessApply` before committing. The project uses Google Java Format (AOSP style).
- **Records:** Prefer Java `record` for DTOs and simple domain models (e.g., `Book`).
- **Validation:** Use `jakarta.validation.constraints` for request body validation.

### Testing Practices
- **JUnit 5:** Primary testing framework.
- **MockMvc:** Used for testing REST controllers in isolation.
- **WebTestClient:** Used in integration tests (e.g., `CatalogServiceApplicationTests`).
- **Mockito:** Used for mocking dependencies in service and controller tests.

### API Endpoints
- `GET /books`: Retrieve the list of all books.
- `GET /books/{isbn}`: Retrieve details for a specific book.
- `POST /books`: Add a new book to the catalog.
- `PUT /books/{isbn}`: Update an existing book's details.
- `DELETE /books/{isbn}`: Remove a book from the catalog.
