# Search Service — Implementation Plan

> **Reference:** `catalog-service` (hexagonal, Spring Data JDBC) + `order-service` (Kafka consumer pattern)
> **Port:** 9005 | **Stack:** Spring Boot 4.0.3 · Spring Data Elasticsearch · Spring Cloud Stream (Kafka)

---

## Target Structure

```
search-service/
├── domain/
│   └── BookDocument.java           ← ES @Document record
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── SearchBooksUseCase.java
│   │   └── out/
│   │       └── BookIndexPort.java  ← index, delete by ISBN
│   └── service/
│       └── BookSearchService.java  ← implements SearchBooksUseCase
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   │   └── SearchController.java
│   │   └── messaging/
│   │       ├── BookEventConsumer.java
│   │       └── messages/
│   │           ├── BookCreatedMessage.java
│   │           ├── BookUpdatedMessage.java
│   │           └── BookDeletedMessage.java
│   └── out/
│       └── elasticsearch/
│           └── ElasticsearchBookRepository.java  ← implements BookIndexPort
└── bootstrap/
    ├── SearchServiceApplication.java
    └── config/
        └── ElasticsearchConfig.java
```

---

## Phase 1 — Bootstrap Project

### 1.1 build.gradle
Dependencies cần thêm:
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-stream'
    implementation 'org.springframework.cloud:spring-cloud-stream-binder-kafka'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
    testImplementation 'org.testcontainers:elasticsearch'
    testImplementation 'org.testcontainers:junit-jupiter'
}
```

### 1.2 application.yml (in config/)
```yaml
# config/search-service.yml
server:
  port: 9005

spring:
  elasticsearch:
    uris: http://localhost:9200
  cloud:
    stream:
      bindings:
        handleBookCreated-in-0:
          destination: book.created
          group: search-service
        handleBookUpdated-in-0:
          destination: book.updated
          group: search-service
        handleBookDeleted-in-0:
          destination: book.deleted
          group: search-service
      function:
        definition: handleBookCreated;handleBookUpdated;handleBookDeleted
```

---

## Phase 2 — Domain

### 2.1 BookDocument.java
```java
@Document(indexName = "books")
public record BookDocument(
    @Id String isbn,
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "english"),
        otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    String title,
    @Field(type = FieldType.Text, analyzer = "english") String author,
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100) Double price,
    @Field(type = FieldType.Keyword) String publisher
) {}
```

---

## Phase 3 — Outbound Port + Elasticsearch Adapter

### 3.1 BookIndexPort.java
```java
public interface BookIndexPort {
    BookDocument save(BookDocument doc);
    void deleteByIsbn(String isbn);
    Optional<BookDocument> findByIsbn(String isbn);
}
```

### 3.2 ElasticsearchBookRepository.java
- Extend `ElasticsearchRepository<BookDocument, String>`
- Implement `BookIndexPort` as a `@Repository` adapter (same pattern as `BookRepositoryImpl` in catalog)

---

## Phase 4 — Application Service

### 4.1 SearchBooksUseCase.java (inbound port)
```java
public interface SearchBooksUseCase {
    Page<BookDocument> search(String query, Pageable pageable);
    Page<BookDocument> searchByAuthor(String author, Pageable pageable);
    List<String> suggest(String prefix);
}
```

### 4.2 BookSearchService.java
- Inject `ElasticsearchOperations` for rich queries (highlight, suggest)
- Inject `BookIndexPort` for write operations
- Methods:
  - `search(String q, Pageable)` → full-text across title + author, returns highlights
  - `searchByAuthor(String author, Pageable)` → keyword filter
  - `suggest(String prefix)` → prefix query on `title.keyword`

---

## Phase 5 — Kafka Consumer (Inbound Adapter)

### 5.1 Message records
```java
// messages consumed from catalog-service topics
public record BookCreatedMessage(String isbn, String title, String author, Double price, String publisher) {}
public record BookUpdatedMessage(String isbn, String title, String author, Double price, String publisher) {}
public record BookDeletedMessage(String isbn) {}
```

### 5.2 BookEventConsumer.java
```java
@Configuration
public class BookEventConsumer {
    @Bean
    public Consumer<BookCreatedMessage> handleBookCreated(BookIndexPort port) {
        return msg -> port.save(new BookDocument(msg.isbn(), msg.title(), msg.author(), msg.price(), msg.publisher()));
    }

    @Bean
    public Consumer<BookUpdatedMessage> handleBookUpdated(BookIndexPort port) {
        return msg -> port.save(new BookDocument(msg.isbn(), msg.title(), msg.author(), msg.price(), msg.publisher()));
    }

    @Bean
    public Consumer<BookDeletedMessage> handleBookDeleted(BookIndexPort port) {
        return msg -> port.deleteByIsbn(msg.isbn());
    }
}
```

---

## Phase 6 — Web Adapter

### 6.1 SearchController.java
Endpoints:
```
GET /search?q=spring&page=0&size=10          ← full-text search with highlights
GET /search?author=vitale&sort=price,asc     ← filter by author + sort
GET /search/suggest?q=spr                    ← autocomplete (returns List<String>)
```

### 6.2 SearchResponse.java DTO
```java
public record SearchResponse(
    String isbn,
    String title,        // may contain <em>highlighted</em> fragments
    String author,
    Double price,
    String publisher
) {}
```

---

## Phase 7 — catalog-service Event Publishing

**Cần thêm vào catalog-service:**

### 7.1 BookEventPublisher (outbound port)
```java
// application/port/out/
public interface BookEventPublisher {
    void publishBookCreated(Book book);
    void publishBookUpdated(Book book);
    void publishBookDeleted(String isbn);
}
```

### 7.2 KafkaBookEventPublisher (adapter/out/messaging/)
- Spring Cloud Stream `StreamBridge`
- Publish to `book.created`, `book.updated`, `book.deleted`

### 7.3 Wire into BookCatalogService
- `addBookToCatalog()` → call `publishBookCreated` after save
- `editBookDetails()` → call `publishBookUpdated` after save
- `deleteBook()` → call `publishBookDeleted` before/after delete

### 7.4 catalog-service build.gradle additions
```groovy
implementation 'org.springframework.cloud:spring-cloud-stream'
implementation 'org.springframework.cloud:spring-cloud-stream-binder-kafka'
```

### 7.5 config/catalog-service.yml additions
```yaml
spring:
  cloud:
    stream:
      bindings:
        book-created-out-0:
          destination: book.created
        book-updated-out-0:
          destination: book.updated
        book-deleted-out-0:
          destination: book.deleted
```

---

## Phase 8 — Infrastructure

### 8.1 Docker Compose additions (polar-deployment/docker/)
```yaml
# Add to compose.yml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
  container_name: polar-elasticsearch
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
  volumes:
    - polar-elasticsearch-data:/usr/share/elasticsearch/data
```

### 8.2 Kubernetes manifests (polar-deployment/kubernetes/local/)
- `elasticsearch-deployment.yml`
- `search-service-deployment.yml`

---

## Phase 9 — Tests

### 9.1 BookSearchServiceTest (unit)
- Mock `BookIndexPort` + `ElasticsearchOperations`
- Test search delegation logic

### 9.2 BookEventConsumerTest (Spring Cloud Stream test binder)
- Verify `book.created` → ES index called
- Verify `book.deleted` → ES delete called
- Same pattern as `OrderEventConsumerTest` in inventory-service

### 9.3 SearchControllerMvcTests (web slice)
- `@WebMvcTest(SearchController.class)`
- Mock `SearchBooksUseCase`
- Test `GET /search?q=spring` → 200 + body

### 9.4 ElasticsearchRepositoryIntegrationTest
```java
@SpringBootTest
@Testcontainers
class ElasticsearchRepositoryIntegrationTest {
    @Container
    static ElasticsearchContainer es = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.0");

    // test save, findByIsbn, deleteByIsbn
}
```

### 9.5 SearchServiceApplicationTests (e2e)
- Full context with Testcontainers ES
- POST book event → search → verify indexed

---

## Implementation Order

| Phase | Task | Risk | Verify |
|-------|------|------|--------|
| 1 | Bootstrap project + build.gradle | ✅ Low | `./gradlew build` |
| 2 | BookDocument domain | ✅ Low | Unit test |
| 3 | Elasticsearch adapter | ⚠️ Medium | TC integration test |
| 4 | Application service | ✅ Low | Mocked unit test |
| 5 | Kafka consumer | ✅ Low | Stream test binder |
| 6 | SearchController | ✅ Low | MockMvc test |
| 7 | catalog-service events | ⚠️ Medium | catalog tests still green |
| 8 | Docker Compose + infra | ✅ Low | `make infra-up` |
| 9 | All tests green | — | `./gradlew test` both services |

---

## Kafka Topic Naming Convention

| Event | Topic | Producer | Consumer |
|-------|-------|----------|---------|
| Book created | `book.created` | catalog-service | search-service |
| Book updated | `book.updated` | catalog-service | search-service |
| Book deleted | `book.deleted` | catalog-service | search-service |

> **Rule:** Follow `<domain>.<event>` convention (same as `order.created`, `order.cancelled` in order-service).
