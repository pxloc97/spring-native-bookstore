package com.locpham.bookstore.orderservice.application.port.out;

import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderQueryPort {
    Flux<Order> findAll();
    Mono<Order> findById(Long id);
    Flux<Order> findByCreatedBy(String createdBy);
}
