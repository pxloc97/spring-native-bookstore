package com.locpham.bookstore.orderservice.adapter.out.messaging;

import com.locpham.bookstore.orderservice.application.port.out.OrderEventPublisherPort;
import com.locpham.bookstore.orderservice.domain.model.Order;
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
}
