package com.locpham.bookstore.orderservice.domain;

import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository {
    Mono<Order> save(Order order);

    Flux<Order> findAll();

    Mono<Order> findById(Long id);

    Flux<Order> findAllByCreatedBy(String createdBy);
}
