package com.locpham.bookstore.dispatcherservice;

import com.locpham.bookstore.dispatcherservice.config.TestObjectMapperConfiguration;
import com.locpham.bookstore.dispatcherservice.message.OrderAcceptedMessage;
import com.locpham.bookstore.dispatcherservice.message.OrderDispatchedMessage;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

@FunctionalSpringBootTest
@Import({TestChannelBinderConfiguration.class, TestObjectMapperConfiguration.class})
public class FunctionalStreamIntegrationTest {
    @Autowired private FunctionCatalog catalog;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void whenOrderAcceptedThenDispatched() {
        long orderId = 121;

        Function<OrderAcceptedMessage, Flux<?>> packAndLabel =
                catalog.lookup(Function.class, "pack|label");

        StepVerifier.create(packAndLabel.apply(new OrderAcceptedMessage(orderId)))
                .expectNextMatches(
                        dispatchedOrderMessage ->
                                isExpectedDispatchedOrder(dispatchedOrderMessage, orderId))
                .verifyComplete();
    }

    private boolean isExpectedDispatchedOrder(Object payload, long orderId) {
        if (payload instanceof OrderDispatchedMessage orderDispatchedMessage) {
            return orderDispatchedMessage.equals(new OrderDispatchedMessage(orderId));
        }
        if (payload instanceof Message<?> message && message.getPayload() instanceof byte[] bytes) {
            try {
                return objectMapper
                        .readValue(bytes, OrderDispatchedMessage.class)
                        .equals(new OrderDispatchedMessage(orderId));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
