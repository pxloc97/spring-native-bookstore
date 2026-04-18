package com.locpham.bookstore.orderservice.application.service;

import com.locpham.bookstore.orderservice.application.command.MarkOrderDispatchedCommand;
import com.locpham.bookstore.orderservice.application.port.in.MarkOrderDispatchedUseCase;
import com.locpham.bookstore.orderservice.application.port.out.OrderCommandPort;
import com.locpham.bookstore.orderservice.application.port.out.OrderQueryPort;
import com.locpham.bookstore.orderservice.domain.model.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class MarkOrderDispatchedService implements MarkOrderDispatchedUseCase {
    private final OrderQueryPort orderQueryPort;
    private final OrderCommandPort orderCommandPort;

    public MarkOrderDispatchedService(
            OrderQueryPort orderQueryPort, OrderCommandPort orderCommandPort) {
        this.orderQueryPort = orderQueryPort;
        this.orderCommandPort = orderCommandPort;
    }

    @Transactional
    @Override
    public Mono<Order> markOrderDispatched(MarkOrderDispatchedCommand command) {
        return orderQueryPort
                .findById(command.orderId())
                .map(Order::markDispatched)
                .flatMap(orderCommandPort::save);
    }
}
