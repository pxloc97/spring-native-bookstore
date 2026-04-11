package com.locpham.bookstore.orderservice.application.port.out;

import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Mono;

public interface OrderEventPublisherPort {
    Mono<Void> publishOrderAccepted(Order order);
}
