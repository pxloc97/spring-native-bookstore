package com.locpham.bookstore.orderservice.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locpham.bookstore.orderservice.application.port.out.OrderEventPublisherPort;
import com.locpham.bookstore.orderservice.domain.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = KafkaOrderEventsPublisherTest.TestApp.class)
class KafkaOrderEventsPublisherTest {

    @Autowired private OrderEventPublisherPort publisher;
    @Autowired private OutputDestination output;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishOrderCreated() throws Exception {
        var order = Order.createPending("1234567890", "Book", 9.99, 2);

        publisher.publishOrderCreated(order).block();

        var message = output.receive(1000, "orderCreated-out-0");
        var payload = new String(message.getPayload());

        var created = objectMapper.readValue(payload, OrderCreatedMessage.class);
        assertEquals(order.id(), created.orderId());
        assertEquals("1234567890", created.items().getFirst().isbn());
        assertEquals(2, created.items().getFirst().quantity());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            excludeName = {
                "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
                "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration",
                "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration",
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration"
            })
    @Import({TestChannelBinderConfiguration.class, KafkaOrderEventPublisher.class})
    static class TestApp {}
}
