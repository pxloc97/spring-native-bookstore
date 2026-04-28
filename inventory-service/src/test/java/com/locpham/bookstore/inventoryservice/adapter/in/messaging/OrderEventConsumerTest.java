package com.locpham.bookstore.inventoryservice.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locpham.bookstore.inventoryservice.TestcontainersConfiguration;
import com.locpham.bookstore.inventoryservice.adapter.in.messaging.messages.OrderCreatedMessage;
import com.locpham.bookstore.inventoryservice.adapter.out.messaging.messages.InventoryDecisionMessage;
import com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq.JooqInventoryRepositoryImpl;
import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
@Testcontainers
class OrderEventConsumerTest {

    @Autowired private InputDestination input;
    @Autowired private OutputDestination output;
    @Autowired private JooqInventoryRepositoryImpl inventoryRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        drainOutput("inventory-events");
    }

    private void drainOutput(String bindingName) {
        Message<byte[]> message;
        do {
            message = output.receive(0, bindingName);
        } while (message != null);
    }

    @Test
    void orderCreated_shouldReserveAndPublishDecision() throws Exception {
        inventoryRepository.save(InventoryItem.create("ABC", 10)).block();

        var orderId = UUID.randomUUID();
        var message =
                new OrderCreatedMessage(
                        orderId, List.of(new OrderCreatedMessage.OrderItem("ABC", 2)));

        input.send(
                MessageBuilder.withPayload(objectMapper.writeValueAsBytes(message))
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build(),
                "order-events");

        var out = output.receive(5000, "inventory-events");
        assertThat(out).isNotNull();

        var decisionMessage =
                objectMapper.readValue(out.getPayload(), InventoryDecisionMessage.class);
        assertThat(decisionMessage.orderId()).isEqualTo(orderId);
        assertThat(decisionMessage.status()).isEqualTo("RESERVED");

        StepVerifier.create(inventoryRepository.findByIsbn("ABC"))
                .assertNext(
                        updated -> {
                            assertThat(updated.availableQuantity()).isEqualTo(8);
                            assertThat(updated.reservedQuantity()).isEqualTo(2);
                        })
                .verifyComplete();
    }

    @Test
    void orderCreated_duplicate_shouldNotDoubleDecrementStock() throws Exception {
        inventoryRepository.save(InventoryItem.create("DUP", 10)).block();

        var orderId = UUID.randomUUID();
        var message =
                new OrderCreatedMessage(
                        orderId, List.of(new OrderCreatedMessage.OrderItem("DUP", 2)));

        var payload =
                MessageBuilder.withPayload(objectMapper.writeValueAsBytes(message))
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();

        input.send(payload, "order-events");
        var first = output.receive(5000, "inventory-events");
        assertThat(first).isNotNull();
        drainOutput("inventory-events");

        input.send(payload, "order-events");

        // Current consumer behavior: duplicate reservation is treated as idempotent and does not
        // re-publish the decision event.
        var second = output.receive(1500, "inventory-events");
        assertThat(second).isNull();

        var eventuallyConsistentStock =
                Mono.defer(() -> inventoryRepository.findByIsbn("DUP"))
                        .filter(
                                updated ->
                                        updated.availableQuantity() == 8
                                                && updated.reservedQuantity() == 2)
                        .repeatWhenEmpty(
                                repeat ->
                                        repeat.delayElements(Duration.ofMillis(100)).take(25))
                        .timeout(Duration.ofSeconds(5));

        StepVerifier.create(eventuallyConsistentStock)
                .assertNext(
                        updated -> {
                            assertThat(updated.availableQuantity()).isEqualTo(8);
                            assertThat(updated.reservedQuantity()).isEqualTo(2);
                        })
                .verifyComplete();
    }
}
