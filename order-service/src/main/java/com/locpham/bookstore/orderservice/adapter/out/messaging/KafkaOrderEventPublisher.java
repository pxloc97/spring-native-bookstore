package com.locpham.bookstore.orderservice.adapter.out.messaging;

import com.locpham.bookstore.orderservice.application.port.out.OrderEventPublisherPort;
import com.locpham.bookstore.orderservice.domain.model.Order;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Primary
public class KafkaOrderEventPublisher implements OrderEventPublisherPort {
    private static final Logger logger = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);
    private final StreamBridge streamBridge;

    public KafkaOrderEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<Void> publishOrderAccepted(Order order) {
        return Mono.fromRunnable(
                () -> {
                    logger.info("Publishing order accepted event: {}", order.id());
                    streamBridge.send("acceptOrder-out-0", new OrderAcceptedMessage(order.id()));
                });
    }

    @Override
    public Mono<Void> publishOrderCreated(Order order) {
        return Mono.fromRunnable(
                () -> {
                    logger.info("Publishing order created event: {}", order.id());
                    var message =
                            new OrderCreatedMessage(
                                    order.id(),
                                    List.of(
                                            new OrderCreatedMessage.OrderItem(
                                                    order.book().isbn(), order.quantity())));
                    streamBridge.send("orderCreated-out-0", message);
                });
    }

    @Override
    public Mono<Void> publishOrderCancelled(Long orderId) {
        return Mono.fromRunnable(
                () -> {
                    logger.info("Publishing order cancelled event: {}", orderId);
                    streamBridge.send("orderCancelled-out-0", new OrderCancelledMessage(orderId));
                });
    }
}
