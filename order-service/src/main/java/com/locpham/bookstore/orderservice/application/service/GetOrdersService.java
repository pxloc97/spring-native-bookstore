package com.locpham.bookstore.orderservice.application.service;

import com.locpham.bookstore.orderservice.application.port.in.GetOrdersUseCase;
import com.locpham.bookstore.orderservice.application.port.out.OrderQueryPort;
import com.locpham.bookstore.orderservice.application.query.GetOrdersQuery;
import com.locpham.bookstore.orderservice.domain.model.Order;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class GetOrdersService implements GetOrdersUseCase {
    private final OrderQueryPort orderQueryPort;

    public GetOrdersService(OrderQueryPort orderQueryPort) {
        this.orderQueryPort = orderQueryPort;
    }

    @Override
    public Flux<Order> getOrders(GetOrdersQuery query) {
        return orderQueryPort.findByCreatedBy(query.userId());
    }
}
