package com.locpham.bookstore.orderservice.application.port.in;

import com.locpham.bookstore.orderservice.application.command.MarkOrderDispatchedCommand;
import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Mono;

public interface MarkOrderDispatchedUseCase {
    Mono<Order> markOrderDispatched(MarkOrderDispatchedCommand command);
}
