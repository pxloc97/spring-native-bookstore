package com.locpham.bookstore.orderservice.application.port.in;

import com.locpham.bookstore.orderservice.application.query.GetOrdersQuery;
import com.locpham.bookstore.orderservice.domain.model.Order;
import reactor.core.publisher.Flux;

public interface GetOrdersUseCase {
    Flux<Order> getOrders(GetOrdersQuery query);
}
