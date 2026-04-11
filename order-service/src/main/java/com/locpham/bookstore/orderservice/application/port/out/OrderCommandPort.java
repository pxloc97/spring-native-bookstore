package com.locpham.bookstore.orderservice.application.port.out;

import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Mono;

public interface OrderCommandPort {
    Mono<Order> save(Order order);
}
