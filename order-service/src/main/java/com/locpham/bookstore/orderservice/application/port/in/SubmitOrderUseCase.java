package com.locpham.bookstore.orderservice.application.port.in;

import com.locpham.bookstore.orderservice.application.command.SubmitOrderCommand;
import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Mono;

public interface SubmitOrderUseCase {
    Mono<Order> submitOrder(SubmitOrderCommand command);
}
