package com.locpham.bookstore.orderservice.adapter.out.messaging.noop;

import com.locpham.bookstore.orderservice.application.port.out.OrderEventPublisherPort;
import com.locpham.bookstore.orderservice.domain.model.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class NoOpOrderEventPublisher implements OrderEventPublisherPort {
    @Override
    public Mono<Void> publishOrderAccepted(Order order) {
        return Mono.empty();
    }
}
