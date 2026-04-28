package com.locpham.bookstore.orderservice.adapter.out.messaging;

import java.util.List;

public record OrderCreatedMessage(Long orderId, List<OrderItem> items) {
    public record OrderItem(String isbn, int quantity) {}
}
